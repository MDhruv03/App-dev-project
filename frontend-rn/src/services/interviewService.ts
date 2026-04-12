import { USE_MOCK_SERVICES, hasApiBaseUrl } from "../config/env";
import { apiRequest } from "../services/apiClient";
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

export type InterviewReactionInput = {
  domain: InterviewDomain;
  topic: InterviewTopic;
  score: number;
  candidateName?: string;
};

export type InterviewReactionResult = {
  phrase: string;
};

function assertInterviewApiReady(): void {
  if (USE_MOCK_SERVICES) {
    throw new Error("Interview evaluation requires backend AI evaluation. Disable EXPO_PUBLIC_USE_MOCKS.");
  }

  if (!hasApiBaseUrl()) {
    throw new Error("Missing API base URL. Start backend and set EXPO_PUBLIC_API_BASE_URL.");
  }
}

export async function evaluateInterviewAnswer(
  input: InterviewEvaluationInput
): Promise<InterviewEvaluationResult> {
  assertInterviewApiReady();

  return apiRequest<InterviewEvaluationResult>("/interview/evaluate", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function fetchInterviewReaction(
  input: InterviewReactionInput
): Promise<InterviewReactionResult> {
  assertInterviewApiReady();

  return apiRequest<InterviewReactionResult>("/interview/react", {
    method: "POST",
    body: JSON.stringify(input),
  });
}
