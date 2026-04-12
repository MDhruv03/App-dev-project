import AsyncStorage from "@react-native-async-storage/async-storage";
import React, { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";
import { USE_MOCK_SERVICES, hasApiBaseUrl } from "../config/env";
import { syncCodingProfiles } from "../services/codingService";
import {
  evaluateInterviewAnswer,
  type InterviewEvaluationResult,
  type InterviewRubric,
} from "../services/interviewService";
import {
  fetchOpportunityFeed,
  type OpportunityRecord,
  type OpportunityType,
} from "../services/opportunitiesService";
import {
  fetchBackendBootstrapState,
  pushBackendStateSnapshot,
} from "../services/stateSyncService";

export type InterviewDomain = "SDE" | "Android" | "Web" | "ML" | "HR";
export type InterviewDifficulty = "Easy" | "Medium" | "Hard";
export type InterviewTopic = "DSA" | "System Design" | "Behavioral" | "Domain";

export type TrackerStatus = "Saved" | "Applied" | "Interview" | "Accepted" | "Rejected";
export type OpportunitySortBy = "recommended" | "deadline" | "match" | "company";

type Contest = {
  title: string;
  time: string;
  duration: string;
};

type TrackerItem = {
  opportunityId: string;
  company: string;
  role: string;
  location: string;
  status: TrackerStatus;
  updatedAt: number;
  interviewScheduledAt: number;
  interviewNote: string;
};

type ActivityLog = {
  id: string;
  type:
    | "save"
    | "unsave"
    | "apply"
    | "status"
    | "interview"
    | "coding"
    | "profile"
    | "discover";
  message: string;
  timestamp: number;
};

type CodingState = {
  leetCodeHandle: string;
  codeforcesHandle: string;
  solved: number;
  mediumHard: number;
  rating: number;
  depth: number;
  status: string;
  contests: Contest[];
  lastSyncedAt: number;
};

type ProfileState = {
  name: string;
  email: string;
  skills: string;
  roles: string;
  savedAt: number;
};

type OpportunityFilter = {
  query: string;
  types: OpportunityType[];
  remoteOnly: boolean;
  paidOnly: boolean;
  savedOnly: boolean;
  sortBy: OpportunitySortBy;
};

type ApplicationRecord = {
  opportunityId: string;
  status: TrackerStatus;
  savedAt: number;
  appliedAt: number;
  interviewScheduledAt: number;
  statusUpdatedAt: number;
  responseDate: number;
  interviewNote: string;
};

type AnalyticsSummary = {
  totalApplications: number;
  savedCount: number;
  interviewCount: number;
  offerCount: number;
  successRate: number;
  avgInterviewScore: number;
  interviewAttempts: number;
};

type InterviewQuestion = {
  id: string;
  prompt: string;
  topic: InterviewTopic;
  hints: string[];
};

type InterviewAnswer = {
  questionId: string;
  audioUri: string;
  durationSec: number;
  transcript: string;
  score: number;
  feedback: string;
  rubric: InterviewRubric;
  strengths: string[];
  improvements: string[];
};

type InterviewSession = {
  active: boolean;
  completed: boolean;
  config: {
    domain: InterviewDomain;
    difficulty: InterviewDifficulty;
    questionCount: number;
    focusTopic: InterviewTopic | "Mixed";
  };
  questions: InterviewQuestion[];
  answers: InterviewAnswer[];
  currentIndex: number;
  startedAt: number;
  endedAt: number;
};

type SubmitResult = {
  score: number;
  feedback: string;
  completed: boolean;
  rubric: InterviewRubric;
  strengths: string[];
  improvements: string[];
};

type SyncCodingResult = {
  ok: boolean;
  message: string;
};

type AppStateValue = {
  profile: ProfileState;
  coding: CodingState;
  opportunities: OpportunityRecord[];
  filteredOpportunities: OpportunityRecord[];
  savedOpportunities: OpportunityRecord[];
  isOpportunityFeedUnlocked: boolean;
  tracker: TrackerItem[];
  activityLog: ActivityLog[];
  interview: InterviewSession;
  analytics: AnalyticsSummary;
  readiness: number;
  roadmapTasks: string[];
  dashboardHeadline: string;
  opportunityFilter: OpportunityFilter;
  isHydrated: boolean;
  isSyncingCoding: boolean;
  isSyncingOpportunities: boolean;
  isSubmittingInterviewAnswer: boolean;
  lastError: string | null;
  clearLastError: () => void;
  updateProfile: (patch: Partial<ProfileState>) => void;
  updateCodingHandles: (handles: { leetCodeHandle: string; codeforcesHandle: string }) => void;
  syncCoding: (handles?: {
    leetCodeHandle: string;
    codeforcesHandle: string;
  }) => Promise<SyncCodingResult>;
  refreshOpportunities: () => Promise<void>;
  setOpportunityQuery: (query: string) => void;
  toggleOpportunityTypeFilter: (type: OpportunityType) => void;
  toggleRemoteOnlyFilter: () => void;
  togglePaidOnlyFilter: () => void;
  toggleSavedOnlyFilter: () => void;
  setOpportunitySortBy: (sortBy: OpportunitySortBy) => void;
  clearOpportunityFilters: () => void;
  toggleSaveOpportunity: (opportunityId: string) => void;
  applyToOpportunity: (opportunityId: string) => void;
  moveApplicationStatus: (opportunityId: string, status: TrackerStatus) => void;
  setInterviewSchedule: (opportunityId: string, timestamp: number, note: string) => void;
  startInterview: (config: {
    domain: InterviewDomain;
    difficulty: InterviewDifficulty;
    questionCount: number;
    focusTopic: InterviewTopic | "Mixed";
  }) => void;
  submitInterviewAnswer: (payload: {
    audioUri: string;
    durationSec: number;
    audioBase64?: string;
    transcript?: string;
  }) => Promise<SubmitResult>;
  endInterviewEarly: () => void;
  resetInterview: () => void;
};

const APP_STATE_STORAGE_KEY_PREFIX = "@madlab/app-state:v2";
const PERSISTENCE_VERSION = 2;
const MAX_PERSISTED_ANSWERS = 30;
const MAX_ACTIVITY_LOGS = 100;

const initialCoding: CodingState = {
  leetCodeHandle: "",
  codeforcesHandle: "",
  solved: 0,
  mediumHard: 0,
  rating: 0,
  depth: 0,
  status: "Connect your coding handles to sync real progress.",
  contests: [],
  lastSyncedAt: 0,
};

const initialProfile: ProfileState = {
  name: "",
  email: "",
  skills: "",
  roles: "",
  savedAt: 0,
};

const initialOpportunityFilter: OpportunityFilter = {
  query: "",
  types: [],
  remoteOnly: false,
  paidOnly: false,
  savedOnly: false,
  sortBy: "recommended",
};

const initialApplications: ApplicationRecord[] = [
  // Start with a quiet tracker and let users curate what they want to load.
];

const neutralRubric: InterviewRubric = {
  content: 0,
  structure: 0,
  clarity: 0,
  confidence: 0,
};

const emptyInterviewSession: InterviewSession = {
  active: false,
  completed: false,
  config: {
    domain: "SDE",
    difficulty: "Medium",
    questionCount: 4,
    focusTopic: "Mixed",
  },
  questions: [],
  answers: [],
  currentIndex: 0,
  startedAt: 0,
  endedAt: 0,
};

type PersistedPayload = {
  version: number;
  isOpportunityFeedUnlocked: boolean;
  profile: ProfileState;
  coding: CodingState;
  opportunities: OpportunityRecord[];
  applications: ApplicationRecord[];
  interview: InterviewSession;
  activityLog: ActivityLog[];
};

type QuestionTemplate = {
  topic: InterviewTopic;
  prompt: string;
  hints: string[];
};

const questionBank: Record<InterviewDomain, QuestionTemplate[]> = {
  SDE: [
    {
      topic: "DSA",
      prompt: "Design a rate limiter for a global API and justify your design choices.",
      hints: ["State throughput assumptions", "Compare token-bucket and sliding-window", "Talk about distributed consistency"],
    },
    {
      topic: "System Design",
      prompt: "How would you build a resilient notification delivery system?",
      hints: ["Queue + retry strategy", "Delivery guarantees", "Monitoring and backpressure"],
    },
    {
      topic: "Behavioral",
      prompt: "Tell me about a production incident you handled under pressure.",
      hints: ["Use STAR structure", "Mention your technical decision", "Share measurable impact"],
    },
    {
      topic: "Domain",
      prompt: "How do you prevent regressions after a risky refactor?",
      hints: ["Testing strategy", "Rollback plan", "Release guardrails"],
    },
  ],
  Android: [
    {
      topic: "Domain",
      prompt: "How do you improve startup time and reduce UI jank in Android apps?",
      hints: ["Profile startup", "Avoid main-thread blocking", "Track frame metrics"],
    },
    {
      topic: "System Design",
      prompt: "Design offline-first sync for a mobile app with unreliable connectivity.",
      hints: ["Conflict resolution", "Retry queue", "Data consistency"],
    },
    {
      topic: "Behavioral",
      prompt: "Describe a hard Android crash you diagnosed and fixed.",
      hints: ["Reproduction strategy", "Root cause", "Prevention"],
    },
    {
      topic: "DSA",
      prompt: "Which data structures matter most in scroll-heavy mobile feeds and why?",
      hints: ["Time complexity", "Memory overhead", "UI impact"],
    },
  ],
  Web: [
    {
      topic: "System Design",
      prompt: "How would you architect a low-latency collaborative editing feature?",
      hints: ["Conflict handling", "Sync model", "Scalability"],
    },
    {
      topic: "Domain",
      prompt: "How do you debug hydration mismatches in production?",
      hints: ["Instrumentation", "Diffing SSR/CSR", "Rollback strategy"],
    },
    {
      topic: "Behavioral",
      prompt: "Tell me about a release where front-end quality regressed and what you changed.",
      hints: ["Root cause", "Team process fix", "Metrics"],
    },
    {
      topic: "DSA",
      prompt: "What algorithmic optimizations have you made for large list rendering?",
      hints: ["Virtualization", "Memoization", "Data access patterns"],
    },
  ],
  ML: [
    {
      topic: "System Design",
      prompt: "Design model monitoring for drift and silent failures.",
      hints: ["Data drift", "Model drift", "Alert thresholds"],
    },
    {
      topic: "Domain",
      prompt: "How do you balance inference latency and accuracy in production?",
      hints: ["SLOs", "Tiered models", "Fallbacks"],
    },
    {
      topic: "Behavioral",
      prompt: "Describe a time a model underperformed and how you recovered.",
      hints: ["Diagnosis path", "Communication", "Long-term fix"],
    },
    {
      topic: "DSA",
      prompt: "Which data structures help optimize feature-store retrieval?",
      hints: ["Indexing", "Caching", "Memory trade-offs"],
    },
  ],
  HR: [
    {
      topic: "Behavioral",
      prompt: "How do you evaluate candidates consistently across interviewers?",
      hints: ["Rubric", "Calibration", "Bias reduction"],
    },
    {
      topic: "Domain",
      prompt: "How would you improve interview quality in a scaling team?",
      hints: ["Training loops", "Feedback quality", "Review cadence"],
    },
    {
      topic: "System Design",
      prompt: "Design an interview loop for speed without compromising quality.",
      hints: ["Stage purpose", "Decision rules", "Throughput"],
    },
    {
      topic: "DSA",
      prompt: "When should algorithm-heavy rounds be weighted less than project-depth rounds?",
      hints: ["Role alignment", "Signal quality", "Fairness"],
    },
  ],
};

const AppStateContext = createContext<AppStateValue | undefined>(undefined);

function clampNumber(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

type RubricDimension = keyof InterviewRubric;

const TRANSCRIPT_STOP_WORDS = new Set([
  "about",
  "after",
  "again",
  "also",
  "because",
  "before",
  "being",
  "between",
  "could",
  "from",
  "have",
  "into",
  "just",
  "like",
  "many",
  "more",
  "most",
  "only",
  "other",
  "over",
  "really",
  "some",
  "than",
  "that",
  "their",
  "there",
  "these",
  "they",
  "this",
  "very",
  "what",
  "when",
  "where",
  "which",
  "while",
  "with",
  "would",
  "your",
]);

function extractTranscriptKeywords(transcript: string): string[] {
  const tokens = transcript
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, " ")
    .split(/\s+/)
    .map((token) => token.trim())
    .filter((token) => token.length >= 4 && !TRANSCRIPT_STOP_WORDS.has(token));

  const frequency = new Map<string, number>();
  for (const token of tokens) {
    frequency.set(token, (frequency.get(token) ?? 0) + 1);
  }

  return [...frequency.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([token]) => token);
}

function getWeakestDimension(rubric: InterviewRubric): RubricDimension {
  const ranked = Object.entries(rubric) as Array<[RubricDimension, number]>;
  ranked.sort((a, b) => a[1] - b[1]);
  return ranked[0][0];
}

function weakestDimensionCoachingCue(dimension: RubricDimension): string {
  if (dimension === "content") {
    return "State one measurable outcome and one explicit trade-off from your decision";
  }
  if (dimension === "structure") {
    return "Answer in this order: context, your action, trade-off, and result";
  }
  if (dimension === "clarity") {
    return "Use short, concrete sentences and avoid jumping between unrelated points";
  }
  return "Sound decisive by naming what you owned, what changed, and why it mattered";
}

function naturalFollowUpByTopic(topic: InterviewTopic): string {
  if (topic === "DSA") {
    return "Walk me through your data-structure choice, then compare one alternative and explain the trade-off.";
  }
  if (topic === "System Design") {
    return "Design this in a production setting: describe architecture, failure modes, and how you would monitor it.";
  }
  if (topic === "Behavioral") {
    return "Tell this like a real story: context, what you personally did, and the measurable outcome.";
  }
  return "Give a concrete real-world example, then justify why your approach was the right call.";
}

function buildAdaptiveFollowUpQuestion(params: {
  previousQuestion: InterviewQuestion;
  transcript: string;
  evaluation: InterviewEvaluationResult;
  domain: InterviewDomain;
  difficulty: InterviewDifficulty;
  nextQuestionNumber: number;
}): InterviewQuestion {
  const transcript = params.transcript.trim();
  const keywords = extractTranscriptKeywords(transcript);
  const weakestDimension = getWeakestDimension(params.evaluation.rubric);
  const coachingCue = weakestDimensionCoachingCue(weakestDimension);
  const firstImprovement = params.evaluation.improvements[0] ?? "Keep your answer evidence-driven.";
  const topicFollowUp = naturalFollowUpByTopic(params.previousQuestion.topic);

  const toneCue =
    params.evaluation.score >= 82
      ? "Nice work. Let us push this one level deeper."
      : params.evaluation.score >= 65
      ? "Good base. Let us make it sharper and more convincing."
      : "No stress. Let us rebuild this answer with stronger structure and confidence.";

  const difficultyCue =
    params.difficulty === "Hard"
      ? "Assume 10x scale, include failure modes, and explain what you would monitor."
      : params.difficulty === "Easy"
      ? "Keep it simple and concrete with one real example."
      : "Include at least one explicit trade-off and one success metric.";

  const contextCue =
    keywords.length > 0
      ? `In your last answer, you mentioned ${keywords.map((item) => `"${item}"`).join(", ")}.`
      : "Let us build directly on your previous response.";

  const prompt = [
    toneCue,
    contextCue,
    `Follow-up question: ${topicFollowUp}`,
    `Coaching cue: ${coachingCue}.`,
    difficultyCue,
  ].join(" ");

  const idSuffix = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 6)}`;

  return {
    id: `${params.domain}-adaptive-${params.nextQuestionNumber}-${idSuffix}`,
    topic: params.previousQuestion.topic,
    prompt,
    hints: [
      "Reference one concrete scenario from your own project work.",
      firstImprovement,
      `Focus dimension: ${weakestDimension}.`,
    ],
  };
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function parseTrackerStatus(value: unknown): TrackerStatus {
  if (
    value === "Saved" ||
    value === "Applied" ||
    value === "Interview" ||
    value === "Accepted" ||
    value === "Rejected"
  ) {
    return value;
  }
  return "Saved";
}

function parseInterviewTopic(value: unknown): InterviewTopic {
  if (
    value === "DSA" ||
    value === "System Design" ||
    value === "Behavioral" ||
    value === "Domain"
  ) {
    return value;
  }
  return "Domain";
}

function parseActivityType(value: unknown): ActivityLog["type"] {
  if (
    value === "save" ||
    value === "unsave" ||
    value === "apply" ||
    value === "status" ||
    value === "interview" ||
    value === "coding" ||
    value === "profile" ||
    value === "discover"
  ) {
    return value;
  }
  return "discover";
}

function sanitizeProfile(raw: unknown): ProfileState {
  if (!isObject(raw)) return initialProfile;

  return {
    name: String(raw.name ?? initialProfile.name),
    email: String(raw.email ?? initialProfile.email),
    skills: String(raw.skills ?? initialProfile.skills),
    roles: String(raw.roles ?? initialProfile.roles),
    savedAt: Number(raw.savedAt ?? initialProfile.savedAt) || 0,
  };
}

function sanitizeCoding(raw: unknown): CodingState {
  if (!isObject(raw)) return initialCoding;

  const contests = Array.isArray(raw.contests)
    ? raw.contests
        .filter((item) => isObject(item))
        .map((item) => ({
          title: String(item.title ?? "Contest"),
          time: String(item.time ?? "TBD"),
          duration: String(item.duration ?? "TBD"),
        }))
    : initialCoding.contests;

  return {
    leetCodeHandle: String(raw.leetCodeHandle ?? initialCoding.leetCodeHandle),
    codeforcesHandle: String(raw.codeforcesHandle ?? initialCoding.codeforcesHandle),
    solved: Number(raw.solved ?? initialCoding.solved) || initialCoding.solved,
    mediumHard: clampNumber(Number(raw.mediumHard ?? initialCoding.mediumHard) || initialCoding.mediumHard, 0, 100),
    rating: Number(raw.rating ?? initialCoding.rating) || initialCoding.rating,
    depth: clampNumber(Number(raw.depth ?? initialCoding.depth) || initialCoding.depth, 0, 100),
    status: String(raw.status ?? initialCoding.status),
    contests: contests.length > 0 ? contests : initialCoding.contests,
    lastSyncedAt: Number(raw.lastSyncedAt ?? initialCoding.lastSyncedAt) || 0,
  };
}

function sanitizeOpportunity(record: unknown): OpportunityRecord | null {
  if (!isObject(record)) return null;
  if (!record.id || !record.title || !record.company) return null;

  const type =
    record.type === "internship" || record.type === "job" || record.type === "hackathon"
      ? record.type
      : "job";

  const experienceLevel =
    record.experienceLevel === "fresher" ||
    record.experienceLevel === "junior" ||
    record.experienceLevel === "mid"
      ? record.experienceLevel
      : "junior";

  const skills = Array.isArray(record.skills)
    ? record.skills.map((skill) => String(skill)).filter(Boolean)
    : [];

  return {
    id: String(record.id),
    title: String(record.title),
    company: String(record.company),
    location: String(record.location ?? "Unknown"),
    remote: Boolean(record.remote),
    paid: Boolean(record.paid),
    type,
    experienceLevel,
    salaryRange: String(record.salaryRange ?? "N/A"),
    skills,
    deadlineEpoch: Number(record.deadlineEpoch ?? Date.now() + 7 * 24 * 60 * 60 * 1000),
    popularity: clampNumber(Number(record.popularity ?? 50), 0, 100),
    matchScore: clampNumber(Number(record.matchScore ?? 50), 0, 100),
    description: String(record.description ?? ""),
  };
}

function sanitizeOpportunities(raw: unknown): OpportunityRecord[] {
  if (!Array.isArray(raw)) return [];

  return raw
    .map((item) => sanitizeOpportunity(item))
    .filter((item): item is OpportunityRecord => item !== null);
}

function sanitizeApplications(raw: unknown): ApplicationRecord[] {
  if (!Array.isArray(raw)) return [];

  const parsed = raw
    .filter((item) => isObject(item))
    .map((item) => ({
      opportunityId: String(item.opportunityId ?? ""),
      status: parseTrackerStatus(item.status),
      savedAt: Number(item.savedAt ?? 0) || 0,
      appliedAt: Number(item.appliedAt ?? 0) || 0,
      interviewScheduledAt: Number(item.interviewScheduledAt ?? 0) || 0,
      statusUpdatedAt: Number(item.statusUpdatedAt ?? 0) || 0,
      responseDate: Number(item.responseDate ?? 0) || 0,
      interviewNote: String(item.interviewNote ?? ""),
    }))
    .filter((item) => item.opportunityId.length > 0);

  return parsed;
}

function sanitizeInterview(raw: unknown): InterviewSession {
  if (!isObject(raw)) return emptyInterviewSession;

  const configRaw = isObject(raw.config) ? raw.config : {};
  const domain =
    configRaw.domain === "SDE" ||
    configRaw.domain === "Android" ||
    configRaw.domain === "Web" ||
    configRaw.domain === "ML" ||
    configRaw.domain === "HR"
      ? configRaw.domain
      : emptyInterviewSession.config.domain;

  const difficulty =
    configRaw.difficulty === "Easy" ||
    configRaw.difficulty === "Medium" ||
    configRaw.difficulty === "Hard"
      ? configRaw.difficulty
      : emptyInterviewSession.config.difficulty;

  const focusTopic =
    configRaw.focusTopic === "Mixed" ||
    configRaw.focusTopic === "DSA" ||
    configRaw.focusTopic === "System Design" ||
    configRaw.focusTopic === "Behavioral" ||
    configRaw.focusTopic === "Domain"
      ? configRaw.focusTopic
      : "Mixed";

  const questionCount = clampNumber(Number(configRaw.questionCount ?? 4) || 4, 1, 10);

  const questions = Array.isArray(raw.questions)
    ? raw.questions
        .filter((item) => isObject(item))
        .map((item) => ({
          id: String(item.id ?? ""),
          prompt: String(item.prompt ?? ""),
          topic: parseInterviewTopic(item.topic),
          hints: Array.isArray(item.hints)
            ? item.hints.map((hint) => String(hint)).filter(Boolean)
            : [],
        }))
        .filter((item) => item.id.length > 0 && item.prompt.length > 0)
    : [];

  const answers = Array.isArray(raw.answers)
    ? raw.answers
        .filter((item) => isObject(item))
        .map((item) => ({
          questionId: String(item.questionId ?? ""),
          audioUri: String(item.audioUri ?? ""),
          durationSec: Number(item.durationSec ?? 0) || 0,
          transcript: String(item.transcript ?? ""),
          score: clampNumber(Number(item.score ?? 0) || 0, 0, 100),
          feedback: String(item.feedback ?? ""),
          rubric: {
            content: clampNumber(Number((item.rubric as Record<string, unknown>)?.content ?? 0), 0, 100),
            structure: clampNumber(Number((item.rubric as Record<string, unknown>)?.structure ?? 0), 0, 100),
            clarity: clampNumber(Number((item.rubric as Record<string, unknown>)?.clarity ?? 0), 0, 100),
            confidence: clampNumber(Number((item.rubric as Record<string, unknown>)?.confidence ?? 0), 0, 100),
          },
          strengths: Array.isArray(item.strengths)
            ? item.strengths.map((entry) => String(entry)).filter(Boolean)
            : [],
          improvements: Array.isArray(item.improvements)
            ? item.improvements.map((entry) => String(entry)).filter(Boolean)
            : [],
        }))
        .filter((item) => item.questionId.length > 0)
    : [];

  const currentIndex = clampNumber(Number(raw.currentIndex ?? 0) || 0, 0, Math.max(questions.length - 1, 0));
  const completed = Boolean(raw.completed);
  const active = Boolean(raw.active) && !completed && questions.length > 0;

  return {
    active,
    completed,
    config: {
      domain,
      difficulty,
      questionCount,
      focusTopic,
    },
    questions,
    answers,
    currentIndex,
    startedAt: Number(raw.startedAt ?? 0) || 0,
    endedAt: Number(raw.endedAt ?? 0) || 0,
  };
}

function sanitizeActivityLog(raw: unknown): ActivityLog[] {
  if (!Array.isArray(raw)) return [];

  return raw
    .filter((item) => isObject(item))
    .map((item) => ({
      id: String(item.id ?? `log-${Date.now()}`),
      type: parseActivityType(item.type),
      message: String(item.message ?? ""),
      timestamp: Number(item.timestamp ?? Date.now()) || Date.now(),
    }))
    .slice(0, MAX_ACTIVITY_LOGS);
}

function buildQuestions(
  domain: InterviewDomain,
  difficulty: InterviewDifficulty,
  focusTopic: InterviewTopic | "Mixed",
  count: number
): InterviewQuestion[] {
  const domainTemplates = questionBank[domain] ?? questionBank.SDE;
  const filtered =
    focusTopic === "Mixed"
      ? domainTemplates
      : domainTemplates.filter((template) => template.topic === focusTopic);

  const source = filtered.length > 0 ? filtered : domainTemplates;
  const difficultyProbe =
    difficulty === "Hard"
      ? "Include scaling constraints, trade-offs, and failure modes."
      : difficulty === "Easy"
      ? "Explain clearly for a junior engineer with one example."
      : "Include one design trade-off and one metric to monitor.";

  const results: InterviewQuestion[] = [];

  for (let index = 0; index < count; index += 1) {
    const template = source[index % source.length];
    results.push({
      id: `${domain}-${template.topic}-${index + 1}`,
      topic: template.topic,
      prompt: `${template.prompt} ${difficultyProbe}`,
      hints: template.hints,
    });
  }

  return results;
}

function sortByStatusWeight(status: TrackerStatus): number {
  const weights: Record<TrackerStatus, number> = {
    Interview: 5,
    Applied: 4,
    Saved: 3,
    Accepted: 2,
    Rejected: 1,
  };
  return weights[status];
}

export function AppStateProvider({
  children,
  userId,
}: {
  children: React.ReactNode;
  userId: string;
}) {
  const appStateStorageKey = `${APP_STATE_STORAGE_KEY_PREFIX}:${userId}`;
  const [profile, setProfile] = useState<ProfileState>(initialProfile);
  const [coding, setCoding] = useState<CodingState>(initialCoding);
  const [opportunities, setOpportunities] = useState<OpportunityRecord[]>([]);
  const [isOpportunityFeedUnlocked, setIsOpportunityFeedUnlocked] = useState(false);
  const [applications, setApplications] = useState<ApplicationRecord[]>(initialApplications);
  const [opportunityFilter, setOpportunityFilter] = useState<OpportunityFilter>(initialOpportunityFilter);
  const [activityLog, setActivityLog] = useState<ActivityLog[]>([]);
  const [interview, setInterview] = useState<InterviewSession>(emptyInterviewSession);
  const [isHydrated, setIsHydrated] = useState(false);
  const [isSyncingCoding, setIsSyncingCoding] = useState(false);
  const [isSyncingOpportunities, setIsSyncingOpportunities] = useState(false);
  const [isSubmittingInterviewAnswer, setIsSubmittingInterviewAnswer] = useState(false);
  const [lastError, setLastError] = useState<string | null>(null);
  const backendSaveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const pushActivity = (
    type: ActivityLog["type"],
    message: string,
    timestamp = Date.now()
  ) => {
    setActivityLog((prev) => {
      const entry: ActivityLog = {
        id: `${timestamp}-${Math.random().toString(36).slice(2, 7)}`,
        type,
        message,
        timestamp,
      };
      return [entry, ...prev].slice(0, MAX_ACTIVITY_LOGS);
    });
  };

  useEffect(() => {
    let isMounted = true;

    async function hydrateState() {
      try {
        const raw = await AsyncStorage.getItem(appStateStorageKey);
        if (isMounted && raw) {
          const parsed = JSON.parse(raw) as PersistedPayload;
          if (parsed.version === PERSISTENCE_VERSION) {
            setIsOpportunityFeedUnlocked(Boolean(parsed.isOpportunityFeedUnlocked));
            setProfile(sanitizeProfile(parsed.profile));
            setCoding(sanitizeCoding(parsed.coding));
            setOpportunities(sanitizeOpportunities(parsed.opportunities));
            setApplications(sanitizeApplications(parsed.applications));
            setInterview(sanitizeInterview(parsed.interview));
            setActivityLog(sanitizeActivityLog(parsed.activityLog));
          }
        }

        if (isMounted && !USE_MOCK_SERVICES && hasApiBaseUrl()) {
          try {
            const remote = await fetchBackendBootstrapState();
            if (!isMounted) {
              return;
            }

            if (remote.profile !== undefined) {
              setProfile(sanitizeProfile(remote.profile));
            }
            if (remote.coding !== undefined) {
              setCoding(sanitizeCoding(remote.coding));
            }
            if (remote.opportunities !== undefined) {
              setOpportunities(sanitizeOpportunities(remote.opportunities));
            }
            if (remote.applications !== undefined) {
              setApplications(sanitizeApplications(remote.applications));
            }
            if (remote.interview !== undefined) {
              setInterview(sanitizeInterview(remote.interview));
            }
            if (remote.activityLog !== undefined) {
              setActivityLog(sanitizeActivityLog(remote.activityLog));
            }
          } catch {
            // Keep local hydration as source of truth when backend bootstrap is unavailable.
          }
        }
      } catch {
        if (isMounted) {
          setLastError("Could not restore local state. Using fresh defaults.");
        }
      } finally {
        if (isMounted) {
          setIsHydrated(true);
        }
      }
    }

    void hydrateState();

    return () => {
      isMounted = false;
    };
  }, [appStateStorageKey]);

  useEffect(() => {
    if (!isHydrated) {
      return;
    }

    const payload: PersistedPayload = {
      version: PERSISTENCE_VERSION,
      isOpportunityFeedUnlocked,
      profile,
      coding,
      opportunities,
      applications,
      interview: {
        ...interview,
        answers: interview.answers.slice(-MAX_PERSISTED_ANSWERS),
      },
      activityLog: activityLog.slice(0, MAX_ACTIVITY_LOGS),
    };

    void AsyncStorage.setItem(appStateStorageKey, JSON.stringify(payload)).catch(() => {
      setLastError("Could not persist local updates.");
    });
  }, [
    appStateStorageKey,
    isHydrated,
    isOpportunityFeedUnlocked,
    profile,
    coding,
    opportunities,
    applications,
    interview,
    activityLog,
  ]);

  useEffect(() => {
    if (!isHydrated || USE_MOCK_SERVICES || !hasApiBaseUrl()) {
      return;
    }

    if (backendSaveTimerRef.current) {
      clearTimeout(backendSaveTimerRef.current);
    }

    const payload = {
      profile,
      coding,
      opportunities,
      applications,
      interview: {
        ...interview,
        answers: interview.answers.slice(-MAX_PERSISTED_ANSWERS),
      },
      activityLog: activityLog.slice(0, MAX_ACTIVITY_LOGS),
    };

    backendSaveTimerRef.current = setTimeout(() => {
      void pushBackendStateSnapshot(payload).catch(() => {
        // Do not block local UX if backend save is temporarily unavailable.
      });
    }, 900);

    return () => {
      if (backendSaveTimerRef.current) {
        clearTimeout(backendSaveTimerRef.current);
        backendSaveTimerRef.current = null;
      }
    };
  }, [isHydrated, profile, coding, opportunities, applications, interview, activityLog]);

  useEffect(() => {
    if (!isHydrated || !isOpportunityFeedUnlocked) {
      return;
    }

    if (opportunities.length > 0) {
      return;
    }

    void (async () => {
      setIsSyncingOpportunities(true);
      try {
        const feed = await fetchOpportunityFeed(profile.skills);
        setOpportunities(feed);
      } catch {
        setLastError("Could not fetch opportunities.");
      } finally {
        setIsSyncingOpportunities(false);
      }
    })();
  }, [isHydrated, isOpportunityFeedUnlocked, opportunities.length, profile.skills]);

  const filteredOpportunities = useMemo(() => {
    if (!isOpportunityFeedUnlocked) {
      return [];
    }

    const savedIds = new Set(
      applications.filter((application) => application.status === "Saved").map((application) => application.opportunityId)
    );

    const query = opportunityFilter.query.trim().toLowerCase();

    let list = opportunities.filter((opportunity) => {
      if (opportunityFilter.types.length > 0 && !opportunityFilter.types.includes(opportunity.type)) {
        return false;
      }

      if (opportunityFilter.remoteOnly && !opportunity.remote) {
        return false;
      }

      if (opportunityFilter.paidOnly && !opportunity.paid) {
        return false;
      }

      if (opportunityFilter.savedOnly && !savedIds.has(opportunity.id)) {
        return false;
      }

      if (!query) {
        return true;
      }

      const haystack = [
        opportunity.title,
        opportunity.company,
        opportunity.location,
        opportunity.description,
        ...opportunity.skills,
      ]
        .join(" ")
        .toLowerCase();

      return haystack.includes(query);
    });

    list = [...list].sort((a, b) => {
      if (opportunityFilter.sortBy === "deadline") {
        return a.deadlineEpoch - b.deadlineEpoch;
      }
      if (opportunityFilter.sortBy === "match") {
        return b.matchScore - a.matchScore;
      }
      if (opportunityFilter.sortBy === "company") {
        return a.company.localeCompare(b.company);
      }

      const aRecommended = Math.round(a.matchScore * 0.65 + a.popularity * 0.35);
      const bRecommended = Math.round(b.matchScore * 0.65 + b.popularity * 0.35);
      return bRecommended - aRecommended;
    });

    return list;
  }, [applications, isOpportunityFeedUnlocked, opportunities, opportunityFilter]);

  const savedOpportunities = useMemo(() => {
    if (!isOpportunityFeedUnlocked) {
      return [];
    }

    const savedIds = new Set(
      applications.filter((application) => application.status === "Saved").map((application) => application.opportunityId)
    );
    return opportunities.filter((opportunity) => savedIds.has(opportunity.id));
  }, [applications, isOpportunityFeedUnlocked, opportunities]);

  const tracker = useMemo(() => {
    return applications
      .map((application) => {
        const opportunity = opportunities.find((item) => item.id === application.opportunityId);
        if (!opportunity) return null;

        return {
          opportunityId: opportunity.id,
          company: opportunity.company,
          role: opportunity.title,
          location: opportunity.location,
          status: application.status,
          updatedAt: application.statusUpdatedAt,
          interviewScheduledAt: application.interviewScheduledAt,
          interviewNote: application.interviewNote,
        } as TrackerItem;
      })
      .filter((item): item is TrackerItem => item !== null)
      .sort((a, b) => {
        const weightDelta = sortByStatusWeight(b.status) - sortByStatusWeight(a.status);
        if (weightDelta !== 0) return weightDelta;
        return b.updatedAt - a.updatedAt;
      });
  }, [applications, opportunities]);

  const analytics = useMemo<AnalyticsSummary>(() => {
    const totalApplications = applications.filter((item) => item.status !== "Saved").length;
    const savedCount = applications.filter((item) => item.status === "Saved").length;
    const interviewCount = applications.filter((item) => item.status === "Interview").length;
    const offerCount = applications.filter((item) => item.status === "Accepted").length;
    const rejectedCount = applications.filter((item) => item.status === "Rejected").length;

    const decided = offerCount + rejectedCount;
    const successRate = decided > 0 ? Math.round((offerCount / decided) * 100) : 0;

    const interviewAttempts = interview.answers.length;
    const avgInterviewScore =
      interviewAttempts > 0
        ? Math.round(interview.answers.reduce((sum, answer) => sum + answer.score, 0) / interviewAttempts)
        : 0;

    return {
      totalApplications,
      savedCount,
      interviewCount,
      offerCount,
      successRate,
      avgInterviewScore,
      interviewAttempts,
    };
  }, [applications, interview.answers]);

  const readiness = useMemo(() => {
    const codingSignal = clampNumber(
      coding.depth * 0.52 + Math.min(34, coding.rating / 36) + Math.min(14, coding.solved / 18),
      0,
      100
    );
    const interviewSignal = analytics.interviewAttempts > 0 ? analytics.avgInterviewScore : 0;
    const pipelineSignal = clampNumber(
      analytics.savedCount * 5 + analytics.totalApplications * 7 + analytics.interviewCount * 12 + analytics.offerCount * 20,
      0,
      100
    );

    const hasCodingSignal =
      codingSignal > 0 ||
      coding.leetCodeHandle.trim().length > 0 ||
      coding.codeforcesHandle.trim().length > 0;
    const hasInterviewSignal = analytics.interviewAttempts > 0;
    const hasPipelineSignal = analytics.savedCount > 0 || analytics.totalApplications > 0;

    let weightedSum = 0;
    let weight = 0;

    if (hasCodingSignal) {
      weightedSum += codingSignal * 0.45;
      weight += 0.45;
    }

    if (hasInterviewSignal) {
      weightedSum += interviewSignal * 0.3;
      weight += 0.3;
    }

    if (hasPipelineSignal) {
      weightedSum += pipelineSignal * 0.25;
      weight += 0.25;
    }

    if (weight === 0) {
      return 0;
    }

    return clampNumber(Math.round(weightedSum / weight), 0, 100);
  }, [
    analytics.avgInterviewScore,
    analytics.interviewAttempts,
    analytics.interviewCount,
    analytics.offerCount,
    analytics.savedCount,
    analytics.totalApplications,
    coding.codeforcesHandle,
    coding.depth,
    coding.leetCodeHandle,
    coding.rating,
    coding.solved,
  ]);

  const dashboardHeadline = useMemo(() => {
    const firstRunNoSignal =
      analytics.savedCount === 0 &&
      analytics.totalApplications === 0 &&
      analytics.interviewAttempts === 0 &&
      coding.solved === 0 &&
      coding.rating === 0;

    if (firstRunNoSignal) {
      return "Welcome. Add your profile and run your first sync or mock interview to start building signal.";
    }

    if (!isOpportunityFeedUnlocked) {
      return "Your feed is currently curated. Tap Refresh Feed when you want to load roles.";
    }

    const nextInterview = tracker
      .filter((item) => item.status === "Interview" && item.interviewScheduledAt > 0)
      .sort((a, b) => a.interviewScheduledAt - b.interviewScheduledAt)[0];

    if (interview.active) {
      return `Mock interview live: Question ${interview.currentIndex + 1} / ${interview.questions.length}`;
    }

    if (nextInterview) {
      return `Upcoming interview: ${nextInterview.company} in ${Math.max(
        0,
        Math.ceil((nextInterview.interviewScheduledAt - Date.now()) / (24 * 60 * 60 * 1000))
      )} day(s).`;
    }

    return `${analytics.totalApplications} active applications. Push one focused practice round today.`;
  }, [
    analytics.interviewAttempts,
    analytics.savedCount,
    analytics.totalApplications,
    coding.rating,
    coding.solved,
    interview.active,
    interview.currentIndex,
    interview.questions.length,
    isOpportunityFeedUnlocked,
    tracker,
  ]);

  const roadmapTasks = useMemo(() => {
    const tasks: string[] = [];

    if (coding.mediumHard < 65) {
      tasks.push("Problem depth sprint: complete 12 medium-hard questions this week.");
    } else {
      tasks.push("Contest execution: 2 contests + 1 upsolve deep-dive this week.");
    }

    if (analytics.interviewAttempts < 3) {
      tasks.push("Run 3 full mock interview rounds with structured answer reviews.");
    } else {
      tasks.push("Refine weak rubric dimensions from latest interview results.");
    }

    if (analytics.totalApplications < 6) {
      tasks.push("Expand pipeline: add 5 high-match opportunities this week.");
    } else {
      tasks.push("Pipeline hygiene: move stale applications to clear outcomes.");
    }

    if (!profile.skills.toLowerCase().includes("system design")) {
      tasks.push("Add system design project proof to profile and resume evidence.");
    } else {
      tasks.push("System design drill: one architecture walkthrough with trade-off narration.");
    }

    tasks.push("Weekly reflection: convert activity logs into 3 measurable improvements.");
    return tasks;
  }, [analytics.interviewAttempts, analytics.totalApplications, coding.mediumHard, profile.skills]);

  const updateProfile = (patch: Partial<ProfileState>) => {
    setProfile((prev) => ({
      ...prev,
      ...patch,
      savedAt: Date.now(),
    }));
    pushActivity("profile", "Profile details updated.");
  };

  const updateCodingHandles = (handles: { leetCodeHandle: string; codeforcesHandle: string }) => {
    setCoding((prev) => ({
      ...prev,
      leetCodeHandle: handles.leetCodeHandle.trim(),
      codeforcesHandle: handles.codeforcesHandle.trim(),
    }));
  };

  const syncCoding = async (handles?: {
    leetCodeHandle: string;
    codeforcesHandle: string;
  }): Promise<SyncCodingResult> => {
    const nextHandles = {
      leetCodeHandle: (handles?.leetCodeHandle ?? coding.leetCodeHandle).trim(),
      codeforcesHandle: (handles?.codeforcesHandle ?? coding.codeforcesHandle).trim(),
    };

    setIsSyncingCoding(true);
    setCoding((prev) => ({
      ...prev,
      leetCodeHandle: nextHandles.leetCodeHandle,
      codeforcesHandle: nextHandles.codeforcesHandle,
      status: "Syncing coding profiles...",
    }));

    try {
      const result = await syncCodingProfiles(nextHandles);
      const usedFallback = result.status.toLowerCase().includes("fallback");
      setCoding((prev) => ({
        ...prev,
        solved: result.solved,
        mediumHard: result.mediumHard,
        rating: result.rating,
        depth: result.depth,
        status: result.status,
        lastSyncedAt: Date.now(),
      }));
      setLastError(usedFallback ? result.status : null);
      pushActivity("coding", result.status);
      return {
        ok: !usedFallback,
        message: result.status,
      };
    } catch {
      const message = "Could not sync coding profiles right now.";
      setCoding((prev) => ({ ...prev, status: message }));
      setLastError(message);
      return {
        ok: false,
        message,
      };
    } finally {
      setIsSyncingCoding(false);
    }
  };

  const refreshOpportunities = async () => {
    setIsSyncingOpportunities(true);
    setIsOpportunityFeedUnlocked(true);
    try {
      const feed = await fetchOpportunityFeed(profile.skills);
      setOpportunities(feed);
      setLastError(null);
      pushActivity("discover", `Opportunity feed refreshed (${feed.length} items).`);
    } catch {
      setLastError("Could not refresh opportunities.");
    } finally {
      setIsSyncingOpportunities(false);
    }
  };

  const setOpportunityQuery = (query: string) => {
    setOpportunityFilter((prev) => ({ ...prev, query }));
  };

  const toggleOpportunityTypeFilter = (type: OpportunityType) => {
    setOpportunityFilter((prev) => {
      const exists = prev.types.includes(type);
      const types = exists ? prev.types.filter((item) => item !== type) : [...prev.types, type];
      return {
        ...prev,
        types,
      };
    });
  };

  const toggleRemoteOnlyFilter = () => {
    setOpportunityFilter((prev) => ({ ...prev, remoteOnly: !prev.remoteOnly }));
  };

  const togglePaidOnlyFilter = () => {
    setOpportunityFilter((prev) => ({ ...prev, paidOnly: !prev.paidOnly }));
  };

  const toggleSavedOnlyFilter = () => {
    setOpportunityFilter((prev) => ({ ...prev, savedOnly: !prev.savedOnly }));
  };

  const setOpportunitySortBy = (sortBy: OpportunitySortBy) => {
    setOpportunityFilter((prev) => ({ ...prev, sortBy }));
  };

  const clearOpportunityFilters = () => {
    setOpportunityFilter(initialOpportunityFilter);
  };

  const toggleSaveOpportunity = (opportunityId: string) => {
    const matchedOpportunity = opportunities.find((item) => item.id === opportunityId);
    if (!matchedOpportunity) {
      return;
    }

    setApplications((prev) => {
      const existing = prev.find((item) => item.opportunityId === opportunityId);
      const now = Date.now();

      if (!existing) {
        pushActivity("save", `Saved ${matchedOpportunity.title} at ${matchedOpportunity.company}.`);
        return [
          {
            opportunityId,
            status: "Saved",
            savedAt: now,
            appliedAt: 0,
            interviewScheduledAt: 0,
            statusUpdatedAt: now,
            responseDate: 0,
            interviewNote: "",
          },
          ...prev,
        ];
      }

      if (existing.status !== "Saved") {
        setLastError("Only opportunities in Saved state can be unsaved.");
        return prev;
      }

      pushActivity("unsave", `Removed saved item: ${matchedOpportunity.title} at ${matchedOpportunity.company}.`);
      return prev.filter((item) => item.opportunityId !== opportunityId);
    });
  };

  const applyToOpportunity = (opportunityId: string) => {
    const matchedOpportunity = opportunities.find((item) => item.id === opportunityId);
    if (!matchedOpportunity) {
      return;
    }

    setApplications((prev) => {
      const now = Date.now();
      const existing = prev.find((item) => item.opportunityId === opportunityId);

      if (!existing) {
        pushActivity("apply", `Applied to ${matchedOpportunity.title} at ${matchedOpportunity.company}.`);
        return [
          {
            opportunityId,
            status: "Applied",
            savedAt: now,
            appliedAt: now,
            interviewScheduledAt: 0,
            statusUpdatedAt: now,
            responseDate: 0,
            interviewNote: "",
          },
          ...prev,
        ];
      }

      if (existing.status === "Applied" || existing.status === "Interview" || existing.status === "Accepted") {
        return prev;
      }

      pushActivity("apply", `Applied to ${matchedOpportunity.title} at ${matchedOpportunity.company}.`);
      return prev.map((item) =>
        item.opportunityId === opportunityId
          ? {
              ...item,
              status: "Applied",
              appliedAt: item.appliedAt || now,
              statusUpdatedAt: now,
            }
          : item
      );
    });
  };

  const moveApplicationStatus = (opportunityId: string, status: TrackerStatus) => {
    const opportunity = opportunities.find((item) => item.id === opportunityId);
    if (!opportunity) {
      return;
    }

    setApplications((prev) => {
      const now = Date.now();
      const existing = prev.find((item) => item.opportunityId === opportunityId);
      if (!existing) {
        return prev;
      }

      const next: ApplicationRecord = {
        ...existing,
        status,
        statusUpdatedAt: now,
        appliedAt: status === "Applied" && !existing.appliedAt ? now : existing.appliedAt,
        interviewScheduledAt:
          status === "Interview" && !existing.interviewScheduledAt
            ? now + 2 * 24 * 60 * 60 * 1000
            : existing.interviewScheduledAt,
        responseDate: status === "Accepted" || status === "Rejected" ? now : existing.responseDate,
      };

      pushActivity(
        "status",
        `Updated ${opportunity.company} (${opportunity.title}) to ${status} status.`
      );

      return prev.map((item) => (item.opportunityId === opportunityId ? next : item));
    });
  };

  const setInterviewSchedule = (opportunityId: string, timestamp: number, note: string) => {
    const opportunity = opportunities.find((item) => item.id === opportunityId);
    if (!opportunity) {
      return;
    }

    setApplications((prev) =>
      prev.map((item) =>
        item.opportunityId === opportunityId
          ? {
              ...item,
              status: "Interview",
              interviewScheduledAt: timestamp,
              interviewNote: note,
              statusUpdatedAt: Date.now(),
            }
          : item
      )
    );

    pushActivity(
      "interview",
      `Interview scheduled for ${opportunity.company} (${opportunity.title}).`
    );
  };

  const startInterview = (config: {
    domain: InterviewDomain;
    difficulty: InterviewDifficulty;
    questionCount: number;
    focusTopic: InterviewTopic | "Mixed";
  }) => {
    const normalizedCount = clampNumber(config.questionCount, 1, 10);
    const questions = buildQuestions(config.domain, config.difficulty, config.focusTopic, normalizedCount);

    setInterview({
      active: questions.length > 0,
      completed: false,
      config: {
        ...config,
        questionCount: normalizedCount,
      },
      questions,
      answers: [],
      currentIndex: 0,
      startedAt: Date.now(),
      endedAt: 0,
    });

    setLastError(null);
    pushActivity("interview", `Started ${config.domain} ${config.difficulty} interview simulation.`);
  };

  const submitInterviewAnswer = async (payload: {
    audioUri: string;
    durationSec: number;
    audioBase64?: string;
    transcript?: string;
  }): Promise<SubmitResult> => {
    const snapshot = interview;
    const activeQuestion = snapshot.questions[snapshot.currentIndex];

    if (!snapshot.active || !activeQuestion) {
      return {
        score: 0,
        feedback: "No active interview question. Start a session first.",
        completed: snapshot.completed,
        rubric: neutralRubric,
        strengths: [],
        improvements: [],
      };
    }

    setIsSubmittingInterviewAnswer(true);

    try {
      const evaluation = await evaluateInterviewAnswer({
        domain: snapshot.config.domain,
        difficulty: snapshot.config.difficulty,
        topic: activeQuestion.topic,
        prompt: activeQuestion.prompt,
        durationSec: payload.durationSec,
        audioUri: payload.audioUri,
        audioBase64: payload.audioBase64,
        transcript: payload.transcript,
      });

      let completed = false;

      setInterview((prev) => {
        const question = prev.questions[prev.currentIndex];
        if (!prev.active || !question) {
          completed = prev.completed;
          return prev;
        }

        const answer: InterviewAnswer = {
          questionId: question.id,
          audioUri: payload.audioUri,
          durationSec: payload.durationSec,
          transcript:
            evaluation.transcript?.trim() ||
            payload.transcript?.trim() ||
            `Voice response captured (${Math.max(1, Math.round(payload.durationSec))}s).`,
          score: evaluation.score,
          feedback: evaluation.feedback,
          rubric: evaluation.rubric,
          strengths: evaluation.strengths,
          improvements: evaluation.improvements,
        };

        const nextAnswers = [...prev.answers, answer];
        const isLast = prev.currentIndex >= prev.questions.length - 1;
        const nextIndex = prev.currentIndex + 1;
        const nextQuestions = [...prev.questions];

        if (!isLast) {
          nextQuestions[nextIndex] = buildAdaptiveFollowUpQuestion({
            previousQuestion: question,
            transcript: evaluation.transcript?.trim() || payload.transcript?.trim() || "",
            evaluation,
            domain: prev.config.domain,
            difficulty: prev.config.difficulty,
            nextQuestionNumber: nextIndex + 1,
          });
        }

        completed = isLast;

        return {
          ...prev,
          questions: nextQuestions,
          answers: nextAnswers,
          currentIndex: isLast ? prev.currentIndex : nextIndex,
          active: !isLast,
          completed: isLast,
          endedAt: isLast ? Date.now() : 0,
        };
      });

      pushActivity("interview", `Interview answer evaluated: ${evaluation.score}/100.`);
      setLastError(null);

      return {
        score: evaluation.score,
        feedback: evaluation.feedback,
        completed,
        rubric: evaluation.rubric,
        strengths: evaluation.strengths,
        improvements: evaluation.improvements,
      };
    } catch (error) {
      const message = error instanceof Error && error.message
        ? error.message
        : "Could not evaluate this answer. Please retry.";
      setLastError(message);
      return {
        score: 0,
        feedback: message,
        completed: snapshot.completed,
        rubric: neutralRubric,
        strengths: [],
        improvements: [],
      };
    } finally {
      setIsSubmittingInterviewAnswer(false);
    }
  };

  const resetInterview = () => {
    setInterview(emptyInterviewSession);
  };

  const endInterviewEarly = () => {
    let ended = false;

    setInterview((prev) => {
      if (!prev.active) {
        return prev;
      }

      ended = true;
      const allAnswered = prev.questions.length > 0 && prev.answers.length >= prev.questions.length;

      return {
        ...prev,
        active: false,
        completed: allAnswered,
        endedAt: Date.now(),
      };
    });

    if (ended) {
      pushActivity("interview", "Interview ended early by candidate.");
      setLastError(null);
    }
  };

  const value: AppStateValue = {
    profile,
    coding,
    opportunities,
    filteredOpportunities,
    savedOpportunities,
    isOpportunityFeedUnlocked,
    tracker,
    activityLog,
    interview,
    analytics,
    readiness,
    roadmapTasks,
    dashboardHeadline,
    opportunityFilter,
    isHydrated,
    isSyncingCoding,
    isSyncingOpportunities,
    isSubmittingInterviewAnswer,
    lastError,
    clearLastError: () => setLastError(null),
    updateProfile,
    updateCodingHandles,
    syncCoding,
    refreshOpportunities,
    setOpportunityQuery,
    toggleOpportunityTypeFilter,
    toggleRemoteOnlyFilter,
    togglePaidOnlyFilter,
    toggleSavedOnlyFilter,
    setOpportunitySortBy,
    clearOpportunityFilters,
    toggleSaveOpportunity,
    applyToOpportunity,
    moveApplicationStatus,
    setInterviewSchedule,
    startInterview,
    submitInterviewAnswer,
    endInterviewEarly,
    resetInterview,
  };

  return <AppStateContext.Provider value={value}>{children}</AppStateContext.Provider>;
}

export function useAppState() {
  const context = useContext(AppStateContext);
  if (!context) {
    throw new Error("useAppState must be used inside AppStateProvider");
  }
  return context;
}
