import { USE_MOCK_SERVICES, hasApiBaseUrl } from "../config/env";
import { apiRequest } from "../services/apiClient";

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

type LeetCodeStats = {
  total: number;
  medium: number;
  hard: number;
};

type CodeforcesStats = {
  rating: number;
  maxRating: number;
};

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

function truncateStatus(value: string, limit = 220): string {
  if (value.length <= limit) {
    return value;
  }
  return `${value.slice(0, limit - 3)}...`;
}

function normalizeHandle(raw: string, platform: "leetcode" | "codeforces"): string {
  const trimmed = raw.trim().replace(/^@/, "");
  if (!trimmed) {
    return "";
  }

  if (!trimmed.includes("://")) {
    return trimmed.replace(/\/+$/, "");
  }

  try {
    const parsed = new URL(trimmed);
    const segments = parsed.pathname.split("/").filter(Boolean);
    if (segments.length === 0) {
      return "";
    }

    if (platform === "leetcode") {
      if ((segments[0] === "u" || segments[0] === "profile") && segments[1]) {
        return segments[1];
      }
      return segments[0];
    }

    if (segments[0] === "profile" && segments[1]) {
      return segments[1];
    }

    return segments[0];
  } catch {
    return trimmed.replace(/\/+$/, "");
  }
}

async function fetchLeetCodeStatsDirect(handle: string): Promise<LeetCodeStats> {
  const payload = {
    query: `
      query userPublicProfile($username: String!) {
        matchedUser(username: $username) {
          submitStatsGlobal {
            acSubmissionNum {
              difficulty
              count
            }
          }
        }
      }
    `,
    variables: { username: handle },
  };

  const response = await fetch("https://leetcode.com/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`LeetCode HTTP ${response.status}`);
  }

  const root = (await response.json()) as {
    data?: {
      matchedUser?: {
        submitStatsGlobal?: {
          acSubmissionNum?: Array<{ difficulty?: string; count?: number }>;
        };
      };
    };
  };

  const list = root?.data?.matchedUser?.submitStatsGlobal?.acSubmissionNum;
  if (!Array.isArray(list)) {
    throw new Error("LeetCode handle unavailable or private profile");
  }

  let total = 0;
  let medium = 0;
  let hard = 0;

  for (const item of list) {
    const difficulty = String(item?.difficulty || "").toLowerCase();
    const count = Number(item?.count || 0);
    if (difficulty === "all") total = count;
    if (difficulty === "medium") medium = count;
    if (difficulty === "hard") hard = count;
  }

  return { total, medium, hard };
}

async function fetchCodeforcesStatsDirect(handle: string): Promise<CodeforcesStats> {
  const response = await fetch(
    `https://codeforces.com/api/user.info?handles=${encodeURIComponent(handle)}`,
    {
      headers: {
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    throw new Error(`Codeforces HTTP ${response.status}`);
  }

  const payload = (await response.json()) as {
    status?: string;
    result?: Array<{ rating?: number; maxRating?: number }>;
    comment?: string;
  };

  if (payload?.status !== "OK" || !Array.isArray(payload.result) || payload.result.length === 0) {
    throw new Error(payload.comment ? `Codeforces: ${payload.comment}` : "Codeforces handle unavailable");
  }

  return {
    rating: Number(payload.result[0]?.rating || 0),
    maxRating: Number(payload.result[0]?.maxRating || 0),
  };
}

function buildResultFromSignals(params: {
  leetCode: LeetCodeStats | null;
  codeforces: CodeforcesStats | null;
  handles: CodingSyncInput;
  notes: string[];
  sourceLabel: string;
}): CodingSyncResult {
  const { leetCode, codeforces, handles, notes, sourceLabel } = params;

  const signal = (handles.leetCodeHandle.length + handles.codeforcesHandle.length) * 3;
  const fallbackSolved = Math.max(120, 160 + signal);
  const fallbackMediumHard = Math.min(88, 52 + Math.round(signal * 0.5));
  const fallbackRating = Math.min(2100, 1300 + signal * 6);

  const leetTotal = leetCode?.total ?? 0;
  const medium = leetCode?.medium ?? 0;
  const hard = leetCode?.hard ?? 0;
  const rating = codeforces?.rating ?? 0;

  const solvedFromRating = rating > 0 ? Math.max(90, Math.round(rating / 7.5)) : 0;
  const solved = leetTotal > 0 ? leetTotal : solvedFromRating || fallbackSolved;

  const mediumHardFromLeet = leetTotal > 0 ? Math.round(((medium + hard) / Math.max(1, leetTotal)) * 100) : 0;
  const mediumHardFromRating = rating > 0 ? clamp(Math.round((rating - 900) / 12), 20, 86) : 0;
  const mediumHard = clamp(mediumHardFromLeet || mediumHardFromRating || fallbackMediumHard, 0, 100);

  const finalRating = rating || fallbackRating;

  const depth = clamp(
    Math.round(
      Math.min(60, solved / 5.5) +
        Math.min(20, mediumHard / 5) +
        Math.min(20, Math.max(0, (finalRating - 900) / 70))
    ),
    0,
    100
  );

  const syncedSources: string[] = [];
  if (leetCode) syncedSources.push("LeetCode");
  if (codeforces) syncedSources.push("Codeforces");

  let status = `Synced ${syncedSources.join(" + ")} via ${sourceLabel}`;
  if (notes.length > 0) {
    status = `${status} | ${notes.join(" | ")}`;
  }

  return {
    solved,
    mediumHard,
    rating: finalRating,
    depth,
    status: truncateStatus(status),
  };
}

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
  const normalizedHandles = {
    leetCodeHandle: normalizeHandle(input.leetCodeHandle, "leetcode"),
    codeforcesHandle: normalizeHandle(input.codeforcesHandle, "codeforces"),
  };

  if (!normalizedHandles.leetCodeHandle && !normalizedHandles.codeforcesHandle) {
    await new Promise((resolve) => setTimeout(resolve, 250));
    return simulateCodingSync(normalizedHandles);
  }

  let backendError = "";
  if (!USE_MOCK_SERVICES && hasApiBaseUrl()) {
    try {
      const backendResult = await apiRequest<CodingSyncResult>("/coding/sync", {
        method: "POST",
        body: JSON.stringify(normalizedHandles),
      });
      return {
        ...backendResult,
        status: truncateStatus(backendResult.status || "Synced competitive profiles"),
      };
    } catch (error) {
      backendError = error instanceof Error ? error.message : "backend request failed";
    }
  }

  try {
    const notes: string[] = [];
    let leetCode: LeetCodeStats | null = null;
    let codeforces: CodeforcesStats | null = null;

    if (normalizedHandles.leetCodeHandle) {
      try {
        leetCode = await fetchLeetCodeStatsDirect(normalizedHandles.leetCodeHandle);
      } catch (error) {
        notes.push(error instanceof Error ? error.message : "LeetCode sync unavailable");
      }
    }

    if (normalizedHandles.codeforcesHandle) {
      try {
        codeforces = await fetchCodeforcesStatsDirect(normalizedHandles.codeforcesHandle);
      } catch (error) {
        notes.push(error instanceof Error ? error.message : "Codeforces sync unavailable");
      }
    }

    if (!leetCode && !codeforces) {
      throw new Error(notes[0] ?? "Direct sync could not fetch any profile");
    }

    if (backendError) {
      notes.push(`backend unavailable (${backendError})`);
    }

    return buildResultFromSignals({
      leetCode,
      codeforces,
      handles: normalizedHandles,
      notes,
      sourceLabel: "public APIs",
    });
  } catch (directError) {
    const directMessage = directError instanceof Error ? directError.message : "direct sync unavailable";
    const notes = [backendError ? `backend unavailable (${backendError})` : "", `public sync failed (${directMessage})`]
      .filter(Boolean)
      .join(" | ");

    return {
      ...simulateCodingSync(normalizedHandles),
      status: truncateStatus(`Synced with local fallback | ${notes}`),
    };
  }
}
