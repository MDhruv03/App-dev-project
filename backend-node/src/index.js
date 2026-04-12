import express from "express";
import cors from "cors";
import { config } from "./config.js";
import { JsonStore } from "./lib/store.js";
import { personalizeAndRankOpportunities } from "./lib/recommendation.js";
import { syncCodingProfiles } from "./lib/codingSync.js";
import { evaluateInterviewAnswer } from "./lib/interviewEval.js";
import { computeAnalytics, computeReadiness, computeRoadmapTasks } from "./lib/analytics.js";

const app = express();
const store = new JsonStore(config.dataFilePath);

await store.init();

const corsOrigin = config.corsOrigin === "*"
  ? true
  : config.corsOrigin.split(",").map((entry) => entry.trim()).filter(Boolean);

app.use(cors({ origin: corsOrigin }));
app.use(express.json({ limit: "2mb" }));

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

app.get("/health", async (_req, res) => {
  const state = await store.getState();
  res.json({
    ok: true,
    service: "opportunityhub-backend-node",
    uptimeSec: Math.round(process.uptime()),
    groqConfigured: Boolean(config.groqApiKey),
    pollyConfigured: Boolean(config.pollyEnabled && config.pollyProxyUrl),
    opportunities: safeArray(state.opportunities).length
  });
});

app.get("/opportunities", async (_req, res) => {
  const state = await store.getState();
  res.json(safeArray(state.opportunities));
});

app.post("/opportunities/feed", async (req, res) => {
  const skills = String(req.body?.skills || "");
  const state = await store.getState();
  const ranked = personalizeAndRankOpportunities(safeArray(state.opportunities), skills);
  res.json(ranked);
});

app.post("/coding/sync", async (req, res) => {
  const result = await syncCodingProfiles(req.body || {});

  await store.update((state) => {
    state.coding = {
      ...state.coding,
      leetCodeHandle: String(req.body?.leetCodeHandle || state.coding?.leetCodeHandle || "").trim(),
      codeforcesHandle: String(req.body?.codeforcesHandle || state.coding?.codeforcesHandle || "").trim(),
      solved: result.solved,
      mediumHard: clamp(result.mediumHard, 0, 100),
      rating: result.rating,
      depth: clamp(result.depth, 0, 100),
      status: result.status,
      lastSyncedAt: Date.now()
    };

    const logEntry = createActivity("coding", result.status);
    state.activityLog = [logEntry, ...safeArray(state.activityLog)].slice(0, 100);
    return state;
  });

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

  await store.update((state) => {
    const answerRecord = {
      questionId: req.body?.questionId || `q-${Date.now()}`,
      audioUri: payload.audioUri,
      durationSec: payload.durationSec,
      transcript: payload.transcript,
      score: evaluation.score,
      feedback: evaluation.feedback,
      rubric: evaluation.rubric,
      strengths: evaluation.strengths,
      improvements: evaluation.improvements
    };

    if (!state.interview || typeof state.interview !== "object") {
      state.interview = {
        active: false,
        completed: false,
        config: {
          domain: payload.domain,
          difficulty: payload.difficulty,
          questionCount: 4,
          focusTopic: "Mixed"
        },
        questions: [],
        answers: [],
        currentIndex: 0,
        startedAt: 0,
        endedAt: 0
      };
    }

    state.interview.answers = [...safeArray(state.interview.answers), answerRecord].slice(-60);
    state.activityLog = [
      createActivity("interview", `Interview answer evaluated: ${evaluation.score}/100.`),
      ...safeArray(state.activityLog)
    ].slice(0, 100);

    return state;
  });

  res.json(evaluation);
});

app.get("/profile", async (_req, res) => {
  const state = await store.getState();
  res.json(state.profile || {});
});

app.patch("/profile", async (req, res) => {
  const patch = req.body || {};

  const nextState = await store.update((state) => {
    state.profile = {
      ...state.profile,
      name: patch.name !== undefined ? String(patch.name) : state.profile?.name,
      email: patch.email !== undefined ? String(patch.email) : state.profile?.email,
      skills: patch.skills !== undefined ? String(patch.skills) : state.profile?.skills,
      roles: patch.roles !== undefined ? String(patch.roles) : state.profile?.roles,
      savedAt: Date.now()
    };

    state.activityLog = [
      createActivity("profile", "Profile details updated."),
      ...safeArray(state.activityLog)
    ].slice(0, 100);

    return state;
  });

  res.json(nextState.profile);
});

app.get("/applications", async (_req, res) => {
  const state = await store.getState();
  res.json(safeArray(state.applications));
});

app.post("/applications", async (req, res) => {
  const now = Date.now();
  const opportunityId = String(req.body?.opportunityId || "").trim();
  const status = normalizeStatus(req.body?.status || "Saved");

  if (!opportunityId) {
    res.status(400).json({ message: "opportunityId is required" });
    return;
  }

  const nextState = await store.update((state) => {
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

    return state;
  });

  const record = safeArray(nextState.applications).find((item) => item.opportunityId === opportunityId);
  res.json(record || null);
});

app.patch("/applications/:opportunityId", async (req, res) => {
  const opportunityId = String(req.params.opportunityId || "").trim();
  if (!opportunityId) {
    res.status(400).json({ message: "opportunityId is required" });
    return;
  }

  const nextState = await store.update((state) => {
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

    return state;
  });

  const record = safeArray(nextState.applications).find((item) => item.opportunityId === opportunityId);
  if (!record) {
    res.status(404).json({ message: "Application not found" });
    return;
  }

  res.json(record);
});

app.delete("/applications/:opportunityId", async (req, res) => {
  const opportunityId = String(req.params.opportunityId || "").trim();
  if (!opportunityId) {
    res.status(400).json({ message: "opportunityId is required" });
    return;
  }

  await store.update((state) => {
    state.applications = safeArray(state.applications).filter((item) => item.opportunityId !== opportunityId);
    state.activityLog = [
      createActivity("unsave", `Application ${opportunityId} removed.`),
      ...safeArray(state.activityLog)
    ].slice(0, 100);
    return state;
  });

  res.json({ ok: true });
});

app.get("/activity", async (_req, res) => {
  const state = await store.getState();
  res.json(safeArray(state.activityLog));
});

app.get("/analytics", async (_req, res) => {
  const state = await store.getState();
  const analytics = computeAnalytics(state);
  const readiness = computeReadiness(state, analytics);
  res.json({ analytics, readiness });
});

app.get("/roadmap", async (_req, res) => {
  const state = await store.getState();
  const analytics = computeAnalytics(state);
  const roadmapTasks = computeRoadmapTasks(state, analytics);
  res.json({ roadmapTasks });
});

app.get("/state/bootstrap", async (_req, res) => {
  const state = await store.getState();
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

app.post("/state/save", async (req, res) => {
  const incoming = req.body || {};

  await store.update((state) => {
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
      state.opportunities = incoming.opportunities;
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

    return state;
  });

  res.json({ ok: true, savedAt: Date.now() });
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
