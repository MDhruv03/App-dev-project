import { USE_MOCK_SERVICES, hasApiBaseUrl } from "../config/env";
import { apiRequest } from "./apiClient";
import type { InterviewDifficulty, InterviewDomain, InterviewTopic } from "../state/AppState";

export type InterviewEvaluationInput = {
  domain: InterviewDomain;
  difficulty: InterviewDifficulty;
  topic: InterviewTopic;
  prompt: string;
  durationSec: number;
  audioUri: string;
  audioBase64?: string;
  transcript?: string;
};

export type InterviewRubric = {
  content: number;
  structure: number;
  clarity: number;
  confidence: number;
};

export type InterviewEvaluationResult = {
  score: number;
  feedback: string;
  rubric: InterviewRubric;
  strengths: string[];
  improvements: string[];
  transcript?: string;
};

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

function durationSignal(durationSec: number): number {
  if (durationSec < 12) return 34;
  if (durationSec < 22) return 48;
  if (durationSec < 38) return 62;
  if (durationSec < 55) return 74;
  if (durationSec < 78) return 84;
  if (durationSec < 105) return 88;
  return 82;
}

function tokenize(text: string): string[] {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, " ")
    .split(/\s+/)
    .map((token) => token.trim())
    .filter(Boolean);
}

function computeTranscriptSignals(transcript?: string): {
  lengthScore: number;
  structureScore: number;
  evidenceScore: number;
  relevanceScore: number;
  confidenceScore: number;
} {
  const text = transcript?.trim() ?? "";
  if (!text) {
    return {
      lengthScore: 10,
      structureScore: 10,
      evidenceScore: 8,
      relevanceScore: 10,
      confidenceScore: 12,
    };
  }

  const tokens = tokenize(text);
  const normalized = text.toLowerCase();

  const lengthScore = clamp(Math.round(tokens.length * 1.2), 10, 28);

  const structureMarkers = ["first", "second", "then", "finally", "because", "therefore", "so that"];
  const structureHits = structureMarkers.filter((marker) => normalized.includes(marker)).length;
  const structureScore = clamp(12 + structureHits * 4, 10, 26);

  const evidenceMarkers = [
    "metric",
    "impact",
    "result",
    "%",
    "latency",
    "throughput",
    "error rate",
    "incident",
    "rollback",
    "trade-off",
  ];
  const evidenceHits = evidenceMarkers.filter((marker) => normalized.includes(marker)).length;
  const evidenceScore = clamp(8 + evidenceHits * 4, 8, 28);

  const relevanceMarkers = [
    "user",
    "system",
    "design",
    "scale",
    "testing",
    "monitor",
    "outcome",
    "ownership",
  ];
  const relevanceHits = relevanceMarkers.filter((marker) => normalized.includes(marker)).length;
  const relevanceScore = clamp(10 + relevanceHits * 3, 10, 24);

  const hedgeWords = ["maybe", "probably", "i guess", "kind of", "sort of", "not sure"];
  const hedgeHits = hedgeWords.filter((word) => normalized.includes(word)).length;
  const confidenceWords = ["implemented", "designed", "measured", "debugged", "led", "improved"];
  const confidenceHits = confidenceWords.filter((word) => normalized.includes(word)).length;
  const confidenceScore = clamp(14 + confidenceHits * 3 - hedgeHits * 4, 8, 24);

  return {
    lengthScore,
    structureScore,
    evidenceScore,
    relevanceScore,
    confidenceScore,
  };
}

function buildLocalRubric(
  score: number,
  signals: ReturnType<typeof computeTranscriptSignals>,
  transcriptProvided: boolean
): InterviewRubric {
  const content = clamp(Math.round(score * 0.45 + signals.evidenceScore + signals.relevanceScore * 0.35), 20, 99);
  const structure = clamp(Math.round(score * 0.38 + signals.structureScore * 1.25), 20, 99);
  const clarity = clamp(Math.round(score * 0.4 + signals.lengthScore * 0.85), 20, 99);
  const confidenceBoost = transcriptProvided ? signals.confidenceScore : 12;
  const confidence = clamp(Math.round(score * 0.38 + confidenceBoost * 1.2), 20, 99);

  return { content, structure, clarity, confidence };
}

function scoreFromLocalSignals(durationSec: number, transcript?: string): number {
  const duration = durationSignal(durationSec);
  const hasTranscript = Boolean(transcript?.trim());
  const signals = computeTranscriptSignals(transcript);

  const transcriptSignal =
    signals.lengthScore +
    signals.structureScore +
    signals.evidenceScore +
    signals.relevanceScore +
    signals.confidenceScore;

  if (!hasTranscript) {
    return clamp(Math.round(duration * 0.75 + 17), 35, 82);
  }

  const transcriptNormalized = clamp(Math.round((transcriptSignal / 130) * 100), 0, 100);
  return clamp(Math.round(duration * 0.36 + transcriptNormalized * 0.64), 35, 97);
}

function buildStrengths(
  score: number,
  rubric: InterviewRubric,
  transcript?: string,
  durationSec?: number
): string[] {
  const strengths: string[] = [];

  if (score >= 82) {
    strengths.push("Strong interview signal with clear ownership and impact framing");
  }
  if (rubric.structure >= 72) {
    strengths.push("Good answer flow with a coherent beginning, middle, and end");
  }
  if ((transcript?.trim().length ?? 0) > 120) {
    strengths.push("Used specific technical vocabulary rather than generic statements");
  }
  if ((durationSec ?? 0) >= 38 && (durationSec ?? 0) <= 85) {
    strengths.push("Response length was in a strong interview-ready range");
  }

  if (strengths.length === 0) {
    strengths.push("Maintained response continuity under interview pressure");
  }

  return strengths.slice(0, 3);
}

function buildImprovements(
  topic: InterviewTopic,
  rubric: InterviewRubric,
  transcriptProvided: boolean,
  durationSec: number
): string[] {
  const improvements: string[] = [];

  if (!transcriptProvided) {
    improvements.push("Add a quick recap in 'What you said' after recording for higher scoring accuracy");
  }
  if (rubric.content < 68) {
    improvements.push("Name one measurable outcome to prove business or user impact");
  }
  if (rubric.structure < 68) {
    improvements.push("Use a clearer sequence: context, your action, trade-off, and result");
  }
  if (durationSec < 28) {
    improvements.push("Expand your answer to around 45-75 seconds to cover depth and reasoning");
  }
  if (topic === "Behavioral") {
    improvements.push("Use STAR explicitly so your behavioral story stays structured");
  }

  if (improvements.length === 0) {
    improvements.push("Maintain this quality and add one stronger metric in your closing sentence");
  }

  return improvements.slice(0, 4);
}

function buildFeedback(score: number, transcriptProvided: boolean): string {
  if (score >= 85) {
    return "Excellent response quality. Keep the same structure and keep emphasizing measurable outcomes.";
  }
  if (score >= 72) {
    return "Good response. Tighten your trade-off explanation and finish with one concrete result metric.";
  }
  if (score >= 60) {
    return transcriptProvided
      ? "Fair response. Improve structure and add more evidence from your real project decisions."
      : "Fair voice response. Add a short recap so scoring can reflect your full content more accurately.";
  }
  return "Current response quality is below interview bar. Slow down, structure your points, and anchor with evidence.";
}

function evaluateAnswerLocally(input: InterviewEvaluationInput): InterviewEvaluationResult {
  const score = scoreFromLocalSignals(input.durationSec, input.transcript);
  const signals = computeTranscriptSignals(input.transcript);
  const transcriptProvided = Boolean(input.transcript?.trim());
  const rubric = buildLocalRubric(score, signals, transcriptProvided);

  return {
    score,
    feedback: buildFeedback(score, transcriptProvided),
    rubric,
    strengths: buildStrengths(score, rubric, input.transcript, input.durationSec),
    improvements: buildImprovements(input.topic, rubric, transcriptProvided, input.durationSec),
    transcript: input.transcript?.trim() || "",
  };
}

export async function evaluateInterviewAnswer(
  input: InterviewEvaluationInput
): Promise<InterviewEvaluationResult> {
  if (USE_MOCK_SERVICES || !hasApiBaseUrl()) {
    await new Promise((resolve) => setTimeout(resolve, 450));
    return evaluateAnswerLocally(input);
  }

  try {
    return await apiRequest<InterviewEvaluationResult>("/interview/evaluate", {
      method: "POST",
      body: JSON.stringify(input),
    });
  } catch {
    return {
      ...evaluateAnswerLocally(input),
      feedback: "Evaluator fallback used. Check backend connectivity for full AI grading.",
    };
  }
}
