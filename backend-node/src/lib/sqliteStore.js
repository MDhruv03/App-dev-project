import fs from "fs/promises";
import path from "path";
import crypto from "crypto";
import { DatabaseSync } from "node:sqlite";
import { buildDefaultState } from "./defaults.js";
import { createSessionToken, normalizeEmail, sanitizeDisplayName } from "./auth.js";

function safeArray(value) {
  return Array.isArray(value) ? value : [];
}

function parseJson(value, fallback) {
  try {
    return value ? JSON.parse(value) : fallback;
  } catch {
    return fallback;
  }
}

function toInteger(value, fallback = 0) {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? Math.round(numeric) : fallback;
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function normalizeStatus(value) {
  const normalized = String(value || "").trim().toLowerCase();
  if (normalized === "saved") return "Saved";
  if (normalized === "applied") return "Applied";
  if (normalized === "interview") return "Interview";
  if (normalized === "accepted") return "Accepted";
  if (normalized === "rejected") return "Rejected";
  return "Saved";
}

export class SqliteStore {
  #dbPath;
  #db;

  constructor(dbPath) {
    this.#dbPath = dbPath;
    this.#db = null;
  }

  async init() {
    if (this.#db) {
      return;
    }

    await fs.mkdir(path.dirname(this.#dbPath), { recursive: true });
    this.#db = new DatabaseSync(this.#dbPath);
    this.#db.exec("PRAGMA foreign_keys = ON;");
    this.#db.exec("PRAGMA journal_mode = WAL;");

    this.#createSchema();
    this.#seedOpportunities();
    this.pruneExpiredSessions();
  }

  #createSchema() {
    this.#db.exec(`
      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        email TEXT NOT NULL UNIQUE,
        password_hash TEXT NOT NULL,
        created_at INTEGER NOT NULL
      );

      CREATE TABLE IF NOT EXISTS sessions (
        token TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        expires_at INTEGER NOT NULL,
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS profiles (
        user_id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        email TEXT NOT NULL,
        skills TEXT NOT NULL,
        roles TEXT NOT NULL,
        saved_at INTEGER NOT NULL,
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS coding_states (
        user_id TEXT PRIMARY KEY,
        leetcode_handle TEXT NOT NULL,
        codeforces_handle TEXT NOT NULL,
        solved INTEGER NOT NULL,
        medium_hard INTEGER NOT NULL,
        rating INTEGER NOT NULL,
        depth INTEGER NOT NULL,
        status TEXT NOT NULL,
        contests_json TEXT NOT NULL,
        last_synced_at INTEGER NOT NULL,
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS interview_states (
        user_id TEXT PRIMARY KEY,
        state_json TEXT NOT NULL,
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS applications (
        user_id TEXT NOT NULL,
        opportunity_id TEXT NOT NULL,
        status TEXT NOT NULL,
        saved_at INTEGER NOT NULL,
        applied_at INTEGER NOT NULL,
        interview_scheduled_at INTEGER NOT NULL,
        status_updated_at INTEGER NOT NULL,
        response_date INTEGER NOT NULL,
        interview_note TEXT NOT NULL,
        PRIMARY KEY(user_id, opportunity_id),
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS activity_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        type TEXT NOT NULL,
        message TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS opportunities (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        company TEXT NOT NULL,
        location TEXT NOT NULL,
        remote INTEGER NOT NULL,
        paid INTEGER NOT NULL,
        type TEXT NOT NULL,
        experience_level TEXT NOT NULL,
        salary_range TEXT NOT NULL,
        skills_json TEXT NOT NULL,
        deadline_epoch INTEGER NOT NULL,
        popularity INTEGER NOT NULL,
        match_score INTEGER NOT NULL,
        description TEXT NOT NULL
      );
    `);
  }

  #seedOpportunities() {
    const countRow = this.#db.prepare("SELECT COUNT(1) AS total FROM opportunities").get();
    if ((countRow?.total ?? 0) > 0) {
      return;
    }

    const defaults = buildDefaultState(Date.now());
    const insert = this.#db.prepare(`
      INSERT INTO opportunities (
        id, title, company, location, remote, paid, type, experience_level,
        salary_range, skills_json, deadline_epoch, popularity, match_score, description
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    this.#runInTransaction(() => {
      for (const opportunity of safeArray(defaults.opportunities)) {
        insert.run(
          String(opportunity.id || ""),
          String(opportunity.title || ""),
          String(opportunity.company || ""),
          String(opportunity.location || ""),
          opportunity.remote ? 1 : 0,
          opportunity.paid ? 1 : 0,
          String(opportunity.type || "job"),
          String(opportunity.experienceLevel || "fresher"),
          String(opportunity.salaryRange || ""),
          JSON.stringify(safeArray(opportunity.skills)),
          toInteger(opportunity.deadlineEpoch, Date.now()),
          toInteger(opportunity.popularity, 50),
          toInteger(opportunity.matchScore, 50),
          String(opportunity.description || "")
        );
      }
    });
  }

  #runInTransaction(work) {
    this.#db.exec("BEGIN IMMEDIATE");
    try {
      work();
      this.#db.exec("COMMIT");
    } catch (error) {
      try {
        this.#db.exec("ROLLBACK");
      } catch {
        // ignore rollback errors to preserve original failure
      }
      throw error;
    }
  }

  pruneExpiredSessions() {
    const now = Date.now();
    this.#db.prepare("DELETE FROM sessions WHERE expires_at <= ?").run(now);
  }

  getUserByEmail(email) {
    const cleanEmail = normalizeEmail(email);
    const row = this.#db.prepare(
      "SELECT id, name, email, password_hash AS passwordHash, created_at AS createdAt FROM users WHERE email = ?"
    ).get(cleanEmail);

    return row || null;
  }

  getUserById(userId) {
    const row = this.#db.prepare("SELECT id, name, email, created_at AS createdAt FROM users WHERE id = ?").get(userId);
    return row || null;
  }

  createUser({ name, email, passwordHash }) {
    const cleanEmail = normalizeEmail(email);
    const cleanName = sanitizeDisplayName(name);
    const now = Date.now();
    const userId = crypto.randomUUID();

    this.#runInTransaction(() => {
      this.#db.prepare(
        "INSERT INTO users (id, name, email, password_hash, created_at) VALUES (?, ?, ?, ?, ?)"
      ).run(userId, cleanName, cleanEmail, String(passwordHash || ""), now);

      this.#initializeUserState(userId, cleanName, cleanEmail, now);
    });

    return {
      id: userId,
      name: cleanName,
      email: cleanEmail,
      createdAt: now,
    };
  }

  #initializeUserState(userId, name, email, now = Date.now()) {
    const defaults = buildDefaultState(now);
    const profile = defaults.profile;
    const coding = defaults.coding;
    const interview = defaults.interview;

    this.#db.prepare(
      `INSERT INTO profiles (user_id, name, email, skills, roles, saved_at)
       VALUES (?, ?, ?, ?, ?, ?)`
    ).run(
      userId,
      name || profile.name,
      email || profile.email,
      String(profile.skills || ""),
      String(profile.roles || ""),
      toInteger(profile.savedAt, 0)
    );

    this.#db.prepare(
      `INSERT INTO coding_states (
        user_id, leetcode_handle, codeforces_handle, solved, medium_hard, rating, depth,
        status, contests_json, last_synced_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).run(
      userId,
      String(coding.leetCodeHandle || ""),
      String(coding.codeforcesHandle || ""),
      toInteger(coding.solved, 0),
      clamp(toInteger(coding.mediumHard, 0), 0, 100),
      toInteger(coding.rating, 0),
      clamp(toInteger(coding.depth, 0), 0, 100),
      String(coding.status || "Not synced yet"),
      JSON.stringify(safeArray(coding.contests)),
      toInteger(coding.lastSyncedAt, 0)
    );

    this.#db.prepare("INSERT INTO interview_states (user_id, state_json) VALUES (?, ?)")
      .run(userId, JSON.stringify(interview));
  }

  createSession(userId, ttlMs) {
    const now = Date.now();
    const expiresAt = now + Math.max(60_000, toInteger(ttlMs, 30 * 24 * 60 * 60 * 1000));
    const token = createSessionToken();

    this.#db.prepare(
      "INSERT INTO sessions (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)"
    ).run(token, userId, now, expiresAt);

    return { token, createdAt: now, expiresAt };
  }

  getSession(token) {
    const cleanToken = String(token || "").trim();
    if (!cleanToken) {
      return null;
    }

    const row = this.#db.prepare(`
      SELECT
        s.token AS token,
        s.user_id AS userId,
        s.created_at AS createdAt,
        s.expires_at AS expiresAt,
        u.id AS id,
        u.name AS name,
        u.email AS email
      FROM sessions s
      JOIN users u ON u.id = s.user_id
      WHERE s.token = ?
    `).get(cleanToken);

    if (!row) {
      return null;
    }

    if (toInteger(row.expiresAt, 0) <= Date.now()) {
      this.deleteSession(cleanToken);
      return null;
    }

    return {
      token: row.token,
      user: {
        id: row.id,
        name: row.name,
        email: row.email,
      },
      createdAt: row.createdAt,
      expiresAt: row.expiresAt,
    };
  }

  deleteSession(token) {
    const cleanToken = String(token || "").trim();
    if (!cleanToken) {
      return;
    }
    this.#db.prepare("DELETE FROM sessions WHERE token = ?").run(cleanToken);
  }

  #ensureUserState(userId) {
    const user = this.getUserById(userId);
    if (!user) {
      throw new Error("User not found");
    }

    const profileExists = this.#db.prepare("SELECT 1 AS ok FROM profiles WHERE user_id = ?").get(userId);
    const codingExists = this.#db.prepare("SELECT 1 AS ok FROM coding_states WHERE user_id = ?").get(userId);
    const interviewExists = this.#db.prepare("SELECT 1 AS ok FROM interview_states WHERE user_id = ?").get(userId);

    if (profileExists && codingExists && interviewExists) {
      return;
    }

    this.#runInTransaction(() => {
      if (!profileExists || !codingExists || !interviewExists) {
        this.#db.prepare("DELETE FROM profiles WHERE user_id = ?").run(userId);
        this.#db.prepare("DELETE FROM coding_states WHERE user_id = ?").run(userId);
        this.#db.prepare("DELETE FROM interview_states WHERE user_id = ?").run(userId);
        this.#initializeUserState(userId, user.name, user.email, Date.now());
      }
    });
  }

  #loadOpportunities() {
    const rows = this.#db.prepare(`
      SELECT
        id,
        title,
        company,
        location,
        remote,
        paid,
        type,
        experience_level AS experienceLevel,
        salary_range AS salaryRange,
        skills_json AS skillsJson,
        deadline_epoch AS deadlineEpoch,
        popularity,
        match_score AS matchScore,
        description
      FROM opportunities
      ORDER BY deadline_epoch ASC
    `).all();

    return rows.map((row) => ({
      id: row.id,
      title: row.title,
      company: row.company,
      location: row.location,
      remote: Boolean(row.remote),
      paid: Boolean(row.paid),
      type: row.type,
      experienceLevel: row.experienceLevel,
      salaryRange: row.salaryRange,
      skills: safeArray(parseJson(row.skillsJson, [])),
      deadlineEpoch: toInteger(row.deadlineEpoch, 0),
      popularity: toInteger(row.popularity, 50),
      matchScore: toInteger(row.matchScore, 50),
      description: row.description,
    }));
  }

  #looksLikeLegacySeededUserState(state) {
    const staticSkills = "Kotlin, React Native, SQL, System Design";
    const staticRoles = "Mobile Engineer, SDE";

    const hasLegacyProfileSeed =
      String(state?.profile?.skills || "") === staticSkills &&
      String(state?.profile?.roles || "") === staticRoles;

    const hasLegacyCodingSeed =
      toInteger(state?.coding?.solved, 0) === 182 &&
      toInteger(state?.coding?.mediumHard, 0) === 64 &&
      toInteger(state?.coding?.rating, 0) === 1462 &&
      toInteger(state?.coding?.depth, 0) === 71 &&
      toInteger(state?.coding?.lastSyncedAt, 0) === 0;

    const ids = safeArray(state?.applications).map((entry) => String(entry?.opportunityId || "")).sort();
    const hasLegacyApplications = ids.length === 3 && ids.join(",") === "opp-001,opp-002,opp-003";
    const hasNoActivity = safeArray(state?.activityLog).length === 0;

    return hasLegacyProfileSeed && hasLegacyCodingSeed && hasLegacyApplications && hasNoActivity;
  }

  #normalizeLegacySeededUserState(state) {
    return {
      ...state,
      profile: {
        ...state.profile,
        skills: "",
        roles: "",
        savedAt: 0,
      },
      coding: {
        ...state.coding,
        solved: 0,
        mediumHard: 0,
        rating: 0,
        depth: 0,
        status: "Connect your coding handles to sync real progress.",
        contests: [],
        lastSyncedAt: 0,
      },
      applications: [],
      activityLog: [],
    };
  }

  getStateForUser(userId) {
    this.#ensureUserState(userId);

    const defaults = buildDefaultState(Date.now());

    const profileRow = this.#db.prepare(
      "SELECT name, email, skills, roles, saved_at AS savedAt FROM profiles WHERE user_id = ?"
    ).get(userId);

    const codingRow = this.#db.prepare(`
      SELECT
        leetcode_handle AS leetCodeHandle,
        codeforces_handle AS codeforcesHandle,
        solved,
        medium_hard AS mediumHard,
        rating,
        depth,
        status,
        contests_json AS contestsJson,
        last_synced_at AS lastSyncedAt
      FROM coding_states
      WHERE user_id = ?
    `).get(userId);

    const interviewRow = this.#db.prepare(
      "SELECT state_json AS stateJson FROM interview_states WHERE user_id = ?"
    ).get(userId);

    const applicationRows = this.#db.prepare(`
      SELECT
        opportunity_id AS opportunityId,
        status,
        saved_at AS savedAt,
        applied_at AS appliedAt,
        interview_scheduled_at AS interviewScheduledAt,
        status_updated_at AS statusUpdatedAt,
        response_date AS responseDate,
        interview_note AS interviewNote
      FROM applications
      WHERE user_id = ?
      ORDER BY status_updated_at DESC, saved_at DESC
    `).all(userId);

    const activityRows = this.#db.prepare(`
      SELECT
        id,
        type,
        message,
        timestamp
      FROM activity_logs
      WHERE user_id = ?
      ORDER BY timestamp DESC, id DESC
      LIMIT 100
    `).all(userId);

    const profile = {
      name: String(profileRow?.name ?? defaults.profile.name),
      email: String(profileRow?.email ?? defaults.profile.email),
      skills: String(profileRow?.skills ?? defaults.profile.skills),
      roles: String(profileRow?.roles ?? defaults.profile.roles),
      savedAt: toInteger(profileRow?.savedAt, 0),
    };

    const coding = {
      leetCodeHandle: String(codingRow?.leetCodeHandle ?? defaults.coding.leetCodeHandle),
      codeforcesHandle: String(codingRow?.codeforcesHandle ?? defaults.coding.codeforcesHandle),
      solved: toInteger(codingRow?.solved, defaults.coding.solved),
      mediumHard: clamp(toInteger(codingRow?.mediumHard, defaults.coding.mediumHard), 0, 100),
      rating: toInteger(codingRow?.rating, defaults.coding.rating),
      depth: clamp(toInteger(codingRow?.depth, defaults.coding.depth), 0, 100),
      status: String(codingRow?.status ?? defaults.coding.status),
      contests: safeArray(parseJson(codingRow?.contestsJson, defaults.coding.contests)),
      lastSyncedAt: toInteger(codingRow?.lastSyncedAt, 0),
    };

    const interview = parseJson(interviewRow?.stateJson, defaults.interview);

    const state = {
      version: 2,
      profile,
      coding,
      opportunities: this.#loadOpportunities(),
      applications: safeArray(applicationRows).map((item) => ({
        opportunityId: String(item.opportunityId || ""),
        status: normalizeStatus(item.status),
        savedAt: toInteger(item.savedAt, 0),
        appliedAt: toInteger(item.appliedAt, 0),
        interviewScheduledAt: toInteger(item.interviewScheduledAt, 0),
        statusUpdatedAt: toInteger(item.statusUpdatedAt, 0),
        responseDate: toInteger(item.responseDate, 0),
        interviewNote: String(item.interviewNote || ""),
      })),
      interview,
      activityLog: safeArray(activityRows).map((entry) => ({
        id: String(entry.id),
        type: String(entry.type || "status"),
        message: String(entry.message || ""),
        timestamp: toInteger(entry.timestamp, Date.now()),
      })),
    };

    if (this.#looksLikeLegacySeededUserState(state)) {
      const normalized = this.#normalizeLegacySeededUserState(state);
      this.saveStateForUser(userId, normalized);
      return normalized;
    }

    return state;
  }

  saveStateForUser(userId, nextState) {
    this.#ensureUserState(userId);

    const defaults = buildDefaultState(Date.now());
    const profileInput = nextState?.profile || {};
    const codingInput = nextState?.coding || {};
    const interviewInput = nextState?.interview || defaults.interview;
    const applicationsInput = safeArray(nextState?.applications);
    const activityInput = safeArray(nextState?.activityLog).slice(0, 100);

    const profile = {
      name: sanitizeDisplayName(profileInput.name ?? defaults.profile.name),
      email: normalizeEmail(profileInput.email ?? defaults.profile.email),
      skills: String(profileInput.skills ?? defaults.profile.skills),
      roles: String(profileInput.roles ?? defaults.profile.roles),
      savedAt: toInteger(profileInput.savedAt, Date.now()),
    };

    const coding = {
      leetCodeHandle: String(codingInput.leetCodeHandle ?? "").trim(),
      codeforcesHandle: String(codingInput.codeforcesHandle ?? "").trim(),
      solved: toInteger(codingInput.solved, 0),
      mediumHard: clamp(toInteger(codingInput.mediumHard, 0), 0, 100),
      rating: toInteger(codingInput.rating, 0),
      depth: clamp(toInteger(codingInput.depth, 0), 0, 100),
      status: String(codingInput.status ?? "Not synced yet"),
      contests: safeArray(codingInput.contests),
      lastSyncedAt: toInteger(codingInput.lastSyncedAt, 0),
    };

    const applications = applicationsInput.map((item) => ({
      opportunityId: String(item?.opportunityId || "").trim(),
      status: normalizeStatus(item?.status),
      savedAt: toInteger(item?.savedAt, 0),
      appliedAt: toInteger(item?.appliedAt, 0),
      interviewScheduledAt: toInteger(item?.interviewScheduledAt, 0),
      statusUpdatedAt: toInteger(item?.statusUpdatedAt, Date.now()),
      responseDate: toInteger(item?.responseDate, 0),
      interviewNote: String(item?.interviewNote || ""),
    })).filter((item) => item.opportunityId);

    const activityLog = activityInput.map((entry) => ({
      type: String(entry?.type || "status"),
      message: String(entry?.message || ""),
      timestamp: toInteger(entry?.timestamp, Date.now()),
    })).filter((entry) => entry.message);

    this.#runInTransaction(() => {
      this.#db.prepare(
        `INSERT INTO profiles (user_id, name, email, skills, roles, saved_at)
         VALUES (?, ?, ?, ?, ?, ?)
         ON CONFLICT(user_id) DO UPDATE SET
           name = excluded.name,
           email = excluded.email,
           skills = excluded.skills,
           roles = excluded.roles,
           saved_at = excluded.saved_at`
      ).run(userId, profile.name, profile.email, profile.skills, profile.roles, profile.savedAt);

      this.#db.prepare("UPDATE users SET name = ?, email = ? WHERE id = ?").run(profile.name, profile.email, userId);

      this.#db.prepare(
        `INSERT INTO coding_states (
          user_id, leetcode_handle, codeforces_handle, solved, medium_hard, rating,
          depth, status, contests_json, last_synced_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(user_id) DO UPDATE SET
          leetcode_handle = excluded.leetcode_handle,
          codeforces_handle = excluded.codeforces_handle,
          solved = excluded.solved,
          medium_hard = excluded.medium_hard,
          rating = excluded.rating,
          depth = excluded.depth,
          status = excluded.status,
          contests_json = excluded.contests_json,
          last_synced_at = excluded.last_synced_at`
      ).run(
        userId,
        coding.leetCodeHandle,
        coding.codeforcesHandle,
        coding.solved,
        coding.mediumHard,
        coding.rating,
        coding.depth,
        coding.status,
        JSON.stringify(coding.contests),
        coding.lastSyncedAt
      );

      this.#db.prepare(
        `INSERT INTO interview_states (user_id, state_json)
         VALUES (?, ?)
         ON CONFLICT(user_id) DO UPDATE SET state_json = excluded.state_json`
      ).run(userId, JSON.stringify(interviewInput));

      this.#db.prepare("DELETE FROM applications WHERE user_id = ?").run(userId);
      const appInsert = this.#db.prepare(`
        INSERT INTO applications (
          user_id, opportunity_id, status, saved_at, applied_at, interview_scheduled_at,
          status_updated_at, response_date, interview_note
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `);
      for (const item of applications) {
        appInsert.run(
          userId,
          item.opportunityId,
          item.status,
          item.savedAt,
          item.appliedAt,
          item.interviewScheduledAt,
          item.statusUpdatedAt,
          item.responseDate,
          item.interviewNote
        );
      }

      this.#db.prepare("DELETE FROM activity_logs WHERE user_id = ?").run(userId);
      const activityInsert = this.#db.prepare(
        "INSERT INTO activity_logs (user_id, type, message, timestamp) VALUES (?, ?, ?, ?)"
      );
      for (const entry of activityLog) {
        activityInsert.run(userId, entry.type, entry.message, entry.timestamp);
      }
    });
  }
}
