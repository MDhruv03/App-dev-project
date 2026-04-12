import express from "express";
import cors from "cors";
import { PDFParse } from "pdf-parse";
import { config } from "./config.js";
import {
  hashPassword,
  isStrongPassword,
  isValidEmail,
  normalizeEmail,
  sanitizeDisplayName,
  verifyPassword,
} from "./lib/auth.js";
import { SqliteStore } from "./lib/sqliteStore.js";
import { personalizeAndRankOpportunities } from "./lib/recommendation.js";
import { syncCodingProfiles } from "./lib/codingSync.js";
import { evaluateInterviewAnswer, generateInterviewReaction } from "./lib/interviewEval.js";
import { computeAnalytics, computeReadiness, computeRoadmapTasks } from "./lib/analytics.js";

const app = express();
const store = new SqliteStore(config.sqliteFilePath);

await store.init();

const corsOrigin = config.corsOrigin === "*"
  ? true
  : config.corsOrigin.split(",").map((entry) => entry.trim()).filter(Boolean);

app.use(cors({ origin: corsOrigin }));
app.use(express.json({ limit: "12mb" }));

function extractBearerToken(headerValue) {
  const raw = String(headerValue || "").trim();
  if (!raw.toLowerCase().startsWith("bearer ")) {
    return "";
  }
  return raw.slice(7).trim();
}

async function requireAuth(req, res, next) {
  try {
    const token = extractBearerToken(req.headers.authorization);
    if (!token) {
      res.status(401).json({ message: "Missing bearer token." });
      return;
    }

    const session = store.getSession(token);
    if (!session) {
      res.status(401).json({ message: "Session expired or invalid. Please login again." });
      return;
    }

    req.authToken = token;
    req.authUser = session.user;
    next();
  } catch (error) {
    next(error);
  }
}

function createActivity(type, message) {
  return {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    type,
    message,
    timestamp: Date.now()
  };
}

function normalizeStatus(status) {
  const normalized = String(status || "").trim().toLowerCase();
  if (normalized === "saved") return "Saved";
  if (normalized === "applied") return "Applied";
  if (normalized === "interview") return "Interview";
  if (normalized === "accepted") return "Accepted";
  if (normalized === "rejected") return "Rejected";
  return "Saved";
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function safeArray(value) {
  return Array.isArray(value) ? value : [];
}

async function callVisionValidationService(image) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), config.visionRequestTimeoutMs);

  try {
    const response = await fetch(config.visionServiceUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ image }),
      signal: controller.signal,
    });

    const contentType = String(response.headers.get("content-type") || "").toLowerCase();
    const payload = contentType.includes("application/json")
      ? await response.json()
      : await response.text();

    if (!response.ok) {
      const message =
        typeof payload === "object" && payload && "message" in payload
          ? String(payload.message)
          : typeof payload === "string" && payload.trim()
          ? payload
          : `Vision service request failed with status ${response.status}`;

      throw new Error(message);
    }

    return payload;
  } catch (error) {
    if (error && typeof error === "object" && "name" in error && error.name === "AbortError") {
      throw new Error("Vision service timeout.");
    }

    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

async function extractResumeTextFromUpload(payload) {
  const fileName = String(payload?.fileName || "resume").trim();
  const mimeType = String(payload?.mimeType || "").toLowerCase();
  const fileBase64 = String(payload?.fileBase64 || "").trim();
  const normalizedBase64 = fileBase64.includes(",") ? fileBase64.split(",").pop() || "" : fileBase64;

  if (!normalizedBase64) {
    throw new Error("fileBase64 is required");
  }

  const buffer = Buffer.from(normalizedBase64, "base64");
  if (!buffer.length) {
    throw new Error("Uploaded resume bytes are empty.");
  }

  const lowerName = fileName.toLowerCase();
  const isPdf = mimeType.includes("pdf") || lowerName.endsWith(".pdf");

  if (isPdf) {
    const parser = new PDFParse({ data: buffer });
    try {
      const parsed = await parser.getText();
      return String(parsed?.text || "").trim();
    } finally {
      try {
        await parser.destroy();
      } catch {
        // Best effort cleanup.
      }
    }
  }

  return buffer.toString("utf8").trim();
}

function getUserId(req) {
  return String(req.authUser?.id || "");
}

function loadUserState(req) {
  return store.getStateForUser(getUserId(req));
}

function saveUserState(req, nextState) {
  store.saveStateForUser(getUserId(req), nextState);
}

app.get("/health", (_req, res) => {
  res.json({
    ok: true,
    service: "opportunityhub-backend-node",
    uptimeSec: Math.round(process.uptime()),
    groqConfigured: Boolean(config.groqApiKey),
    pollyConfigured: Boolean(config.pollyEnabled && config.pollyProxyUrl)
  });
});

app.post("/auth/signup", async (req, res, next) => {
  const name = sanitizeDisplayName(req.body?.name);
  const email = normalizeEmail(req.body?.email);
  const password = String(req.body?.password || "");

  if (!isValidEmail(email)) {
    res.status(400).json({ message: "Enter a valid email address." });
    return;
  }

  if (!isStrongPassword(password)) {
    res.status(400).json({ message: "Password must be at least 8 characters." });
    return;
  }

  const existing = store.getUserByEmail(email);
  if (existing) {
    res.status(409).json({ message: "Email is already registered." });
    return;
  }

  try {
    const passwordHash = await hashPassword(password);
    const user = store.createUser({ name, email, passwordHash });
    const session = store.createSession(user.id, config.authSessionTtlMs);

    res.json({
      token: session.token,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
      },
    });
  } catch (error) {
    if (String(error?.message || "").toLowerCase().includes("unique")) {
      res.status(409).json({ message: "Email is already registered." });
      return;
    }
    next(error);
  }
});

app.post("/auth/login", async (req, res) => {
  const email = normalizeEmail(req.body?.email);
  const password = String(req.body?.password || "");

  if (!isValidEmail(email) || !password) {
    res.status(400).json({ message: "Email and password are required." });
    return;
  }

  const user = store.getUserByEmail(email);
  if (!user) {
    res.status(401).json({ message: "Invalid email or password." });
    return;
  }

  const valid = await verifyPassword(password, user.passwordHash);
  if (!valid) {
    res.status(401).json({ message: "Invalid email or password." });
    return;
  }

  const session = store.createSession(user.id, config.authSessionTtlMs);
  res.json({
    token: session.token,
    user: {
      id: user.id,
      name: user.name,
      email: user.email,
    },
  });
});

app.get("/auth/me", requireAuth, (req, res) => {
  res.json({
    user: {
      id: req.authUser.id,
      name: req.authUser.name,
      email: req.authUser.email,
    },
  });
});

app.post("/auth/logout", requireAuth, (req, res) => {
  store.deleteSession(req.authToken);
  res.json({ ok: true });
});

app.use(requireAuth);

app.post("/vision/validate", async (req, res) => {
  const image = String(req.body?.image || "").trim();
  if (!image) {
    res.status(400).json({ message: "image is required" });
    return;
  }

  try {
    const payload = await callVisionValidationService(image);
    res.json(payload);
  } catch (error) {
    const message = error instanceof Error ? error.message : "Vision validation failed.";
    res.status(502).json({ message });
  }
});

app.get("/opportunities", (req, res) => {
  const state = loadUserState(req);
  res.json(safeArray(state.opportunities));
});

app.post("/opportunities/feed", (req, res) => {
  const state = loadUserState(req);
  const skills = String(req.body?.skills || state.profile?.skills || "");
  const ranked = personalizeAndRankOpportunities(safeArray(state.opportunities), skills);
  res.json(ranked);
});

app.post("/coding/sync", async (req, res) => {
  const result = await syncCodingProfiles(req.body || {});
  const state = loadUserState(req);

  state.coding = {
    ...state.coding,
    leetCodeHandle: String(req.body?.leetCodeHandle || state.coding?.leetCodeHandle || "").trim(),
    codeforcesHandle: String(req.body?.codeforcesHandle || state.coding?.codeforcesHandle || "").trim(),
    solved: result.solved,
    mediumHard: clamp(result.mediumHard, 0, 100),
    rating: result.rating,
    depth: clamp(result.depth, 0, 100),
    status: result.status,
    lastSyncedAt: Date.now(),
  };

  state.activityLog = [
    createActivity("coding", result.status),
    ...safeArray(state.activityLog),
  ].slice(0, 100);

  saveUserState(req, state);

  res.json(result);
});

app.post("/interview/evaluate", async (req, res) => {
  const payload = {
    domain: String(req.body?.domain || "SDE"),
    difficulty: String(req.body?.difficulty || "Medium"),
    topic: String(req.body?.topic || "Domain"),
    prompt: String(req.body?.prompt || ""),
    durationSec: Number(req.body?.durationSec || 0),
    audioUri: String(req.body?.audioUri || ""),
    audioBase64: typeof req.body?.audioBase64 === "string" ? req.body.audioBase64 : "",
    transcript: String(req.body?.transcript || "")
  };

  if (!payload.prompt) {
    res.status(400).json({ message: "prompt is required" });
    return;
  }

  const evaluation = await evaluateInterviewAnswer(payload, {
    groqApiKey: config.groqApiKey,
    groqModel: config.groqModel
  });

  const state = loadUserState(req);

  const answerRecord = {
    questionId: req.body?.questionId || `q-${Date.now()}`,
    audioUri: payload.audioUri,
    durationSec: payload.durationSec,
    transcript: evaluation.transcript || payload.transcript,
    score: evaluation.score,
    feedback: evaluation.feedback,
    rubric: evaluation.rubric,
    strengths: evaluation.strengths,
    improvements: evaluation.improvements,
  };

  if (!state.interview || typeof state.interview !== "object") {
    state.interview = {
      active: false,
      completed: false,
      config: {
        domain: payload.domain,
        difficulty: payload.difficulty,
        questionCount: 4,
        focusTopic: "Mixed",
      },
      questions: [],
      answers: [],
      currentIndex: 0,
      startedAt: 0,
      endedAt: 0,
    };
  }

  state.interview.answers = [...safeArray(state.interview.answers), answerRecord].slice(-60);
  state.activityLog = [
    createActivity("interview", `Interview answer evaluated: ${evaluation.score}/100.`),
    ...safeArray(state.activityLog),
  ].slice(0, 100);

  saveUserState(req, state);

  res.json(evaluation);
});

app.post("/interview/react", async (req, res) => {
  const payload = {
    domain: String(req.body?.domain || "SDE"),
    topic: String(req.body?.topic || "Domain"),
    score: Number(req.body?.score || 0),
    candidateName: String(req.body?.candidateName || "")
  };

  const reaction = await generateInterviewReaction(payload, {
    groqApiKey: config.groqApiKey,
    groqModel: config.groqModel
  });

  res.json(reaction);
});

app.get("/profile", (req, res) => {
  const state = loadUserState(req);
  res.json(state.profile || {});
});

app.patch("/profile", (req, res, next) => {
  const patch = req.body || {};

  try {
    const nextState = loadUserState(req);
    nextState.profile = {
      ...nextState.profile,
      name: patch.name !== undefined ? String(patch.name) : nextState.profile?.name,
      email: patch.email !== undefined ? String(patch.email) : nextState.profile?.email,
      skills: patch.skills !== undefined ? String(patch.skills) : nextState.profile?.skills,
      roles: patch.roles !== undefined ? String(patch.roles) : nextState.profile?.roles,
      savedAt: Date.now()
    };

    nextState.activityLog = [
      createActivity("profile", "Profile details updated."),
      ...safeArray(nextState.activityLog)
    ].slice(0, 100);

    saveUserState(req, nextState);
    res.json(nextState.profile);
  } catch (error) {
    if (String(error?.message || "").toLowerCase().includes("unique")) {
      res.status(409).json({ message: "That email is already used by another account." });
      return;
    }
    next(error);
  }
});

app.post("/profile/parse-resume", async (req, res) => {
  try {
    const text = await extractResumeTextFromUpload(req.body || {});
    if (!text) {
      res.status(422).json({ message: "Could not extract readable text from uploaded resume." });
      return;
    }

    res.json({
      text: text.slice(0, 220000),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Resume parsing failed.";
    const status = message.includes("fileBase64") ? 400 : 422;
    res.status(status).json({ message });
  }
});

app.get("/applications", (req, res) => {
  const state = loadUserState(req);
  res.json(safeArray(state.applications));
});

app.post("/applications", async (req, res) => {
  const now = Date.now();
  const opportunityId = String(req.body?.opportunityId || "").trim();
  const status = normalizeStatus(req.body?.status || "Saved");
  const state = loadUserState(req);

  if (!opportunityId) {
    res.status(400).json({ message: "opportunityId is required" });
    return;
  }

  const existing = safeArray(state.applications).find((item) => item.opportunityId === opportunityId);

  if (existing) {
    existing.status = status;
    existing.statusUpdatedAt = now;
    existing.appliedAt = status === "Applied" && !existing.appliedAt ? now : existing.appliedAt;
    existing.interviewScheduledAt =
      status === "Interview" && !existing.interviewScheduledAt
        ? now + 2 * 24 * 60 * 60 * 1000
        : existing.interviewScheduledAt;
    existing.interviewNote = req.body?.interviewNote !== undefined
      ? String(req.body.interviewNote)
      : existing.interviewNote;
  } else {
    state.applications = [
      {
        opportunityId,
        status,
        savedAt: now,
        appliedAt: status === "Saved" ? 0 : now,
        interviewScheduledAt: status === "Interview" ? now + 2 * 24 * 60 * 60 * 1000 : 0,
        statusUpdatedAt: now,
        responseDate: status === "Accepted" || status === "Rejected" ? now : 0,
        interviewNote: String(req.body?.interviewNote || "")
      },
      ...safeArray(state.applications)
    ];
  }

  state.activityLog = [
    createActivity("status", `Application ${opportunityId} updated to ${status}.`),
    ...safeArray(state.activityLog)
  ].slice(0, 100);

  saveUserState(req, state);

  const record = safeArray(state.applications).find((item) => item.opportunityId === opportunityId);
  res.json(record || null);
});

app.patch("/applications/:opportunityId", async (req, res) => {
  const opportunityId = String(req.params.opportunityId || "").trim();
  const state = loadUserState(req);
  if (!opportunityId) {
    res.status(400).json({ message: "opportunityId is required" });
    return;
  }

  state.applications = safeArray(state.applications).map((item) => {
    if (item.opportunityId !== opportunityId) return item;

    const nextStatus = req.body?.status !== undefined ? normalizeStatus(req.body.status) : item.status;
    const now = Date.now();

    return {
      ...item,
      status: nextStatus,
      statusUpdatedAt: now,
      interviewScheduledAt:
        req.body?.interviewScheduledAt !== undefined
          ? Number(req.body.interviewScheduledAt || 0)
          : item.interviewScheduledAt,
      interviewNote: req.body?.interviewNote !== undefined
        ? String(req.body.interviewNote)
        : item.interviewNote,
      appliedAt: nextStatus === "Applied" && !item.appliedAt ? now : item.appliedAt,
      responseDate:
        nextStatus === "Accepted" || nextStatus === "Rejected"
          ? item.responseDate || now
          : item.responseDate
    };
  });

  state.activityLog = [
    createActivity("status", `Application ${opportunityId} patched.`),
    ...safeArray(state.activityLog)
  ].slice(0, 100);

  saveUserState(req, state);

  const record = safeArray(state.applications).find((item) => item.opportunityId === opportunityId);
  if (!record) {
    res.status(404).json({ message: "Application not found" });
    return;
  }

  res.json(record);
});

app.delete("/applications/:opportunityId", async (req, res) => {
  const opportunityId = String(req.params.opportunityId || "").trim();
  const state = loadUserState(req);
  if (!opportunityId) {
    res.status(400).json({ message: "opportunityId is required" });
    return;
  }

  state.applications = safeArray(state.applications).filter((item) => item.opportunityId !== opportunityId);
  state.activityLog = [
    createActivity("unsave", `Application ${opportunityId} removed.`),
    ...safeArray(state.activityLog)
  ].slice(0, 100);
  saveUserState(req, state);

  res.json({ ok: true });
});

app.get("/activity", (req, res) => {
  const state = loadUserState(req);
  res.json(safeArray(state.activityLog));
});

app.get("/analytics", (req, res) => {
  const state = loadUserState(req);
  const analytics = computeAnalytics(state);
  const readiness = computeReadiness(state, analytics);
  res.json({ analytics, readiness });
});

app.get("/roadmap", (req, res) => {
  const state = loadUserState(req);
  const analytics = computeAnalytics(state);
  const roadmapTasks = computeRoadmapTasks(state, analytics);
  res.json({ roadmapTasks });
});

app.get("/state/bootstrap", (req, res) => {
  const state = loadUserState(req);
  const analytics = computeAnalytics(state);
  const readiness = computeReadiness(state, analytics);
  const roadmapTasks = computeRoadmapTasks(state, analytics);

  res.json({
    profile: state.profile,
    coding: state.coding,
    opportunities: state.opportunities,
    applications: state.applications,
    interview: state.interview,
    activityLog: state.activityLog,
    analytics,
    readiness,
    roadmapTasks
  });
});

app.post("/state/save", (req, res, next) => {
  const incoming = req.body || {};
  const state = loadUserState(req);

  try {
    if (incoming.profile && typeof incoming.profile === "object") {
      state.profile = {
        ...state.profile,
        ...incoming.profile,
        savedAt: Date.now()
      };
    }

    if (incoming.coding && typeof incoming.coding === "object") {
      state.coding = {
        ...state.coding,
        ...incoming.coding
      };
    }

    if (Array.isArray(incoming.opportunities)) {
      // Opportunities are global reference data and are not overridden by clients.
    }

    if (Array.isArray(incoming.applications)) {
      state.applications = incoming.applications;
    }

    if (incoming.interview && typeof incoming.interview === "object") {
      state.interview = {
        ...state.interview,
        ...incoming.interview
      };
    }

    if (Array.isArray(incoming.activityLog)) {
      state.activityLog = incoming.activityLog.slice(0, 100);
    }

    saveUserState(req, state);
    res.json({ ok: true, savedAt: Date.now() });
  } catch (error) {
    if (String(error?.message || "").toLowerCase().includes("unique")) {
      res.status(409).json({ message: "That email is already used by another account." });
      return;
    }
    next(error);
  }
});

app.use((error, _req, res, _next) => {
  console.error(error);
  res.status(500).json({ message: "Internal server error" });
});

app.listen(config.port, () => {
  console.log(`OpportunityHub backend listening on http://localhost:${config.port}`);
  console.log(`Groq configured: ${Boolean(config.groqApiKey)}`);
  console.log(`Polly configured: ${Boolean(config.pollyEnabled && config.pollyProxyUrl)}`);
});
