import { USE_MOCK_SERVICES, hasApiBaseUrl } from "../config/env";
import { apiRequest } from "./apiClient";

export type CodingSyncInput = {
  leetCodeHandle: string;
  codeforcesHandle: string;
};

export type CodingSyncResult = {
  solved: number;
  mediumHard: number;
  rating: number;
  depth: number;
  status: string;
};

function simulateCodingSync(input: CodingSyncInput): CodingSyncResult {
  const signal = (input.leetCodeHandle.length + input.codeforcesHandle.length) * 3;
  const solved = Math.max(120, 160 + signal);
  const mediumHard = Math.min(88, 52 + Math.round(signal * 0.5));
  const rating = Math.min(2100, 1300 + signal * 6);
  const depth = Math.min(94, 60 + Math.round(signal * 0.4));

  return {
    solved,
    mediumHard,
    rating,
    depth,
    status:
      input.leetCodeHandle.trim() || input.codeforcesHandle.trim()
        ? "Synced competitive profiles"
        : "Add at least one handle first",
  };
}

export async function syncCodingProfiles(input: CodingSyncInput): Promise<CodingSyncResult> {
  if (USE_MOCK_SERVICES || !hasApiBaseUrl()) {
    await new Promise((resolve) => setTimeout(resolve, 600));
    return simulateCodingSync(input);
  }

  try {
    return await apiRequest<CodingSyncResult>("/coding/sync", {
      method: "POST",
      body: JSON.stringify(input),
    });
  } catch {
    return {
      ...simulateCodingSync(input),
      status: "Synced with local fallback (API unavailable)",
    };
  }
}
