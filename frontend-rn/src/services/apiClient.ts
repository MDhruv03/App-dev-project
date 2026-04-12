import { API_BASE_URL, API_FALLBACK_URL, API_TIMEOUT_MS, hasApiBaseUrl } from "../config/env";

export class ApiError extends Error {
  status: number;

  constructor(message: string, status = 500) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

type ApiRequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  headers?: Record<string, string>;
  body?: string;
};

let authToken: string | null = null;

export function setApiAuthToken(token: string | null | undefined): void {
  const normalized = String(token || "").trim();
  authToken = normalized || null;
}

export function getApiAuthToken(): string | null {
  return authToken;
}

function normalizeBaseUrl(baseUrl: string): string {
  const trimmed = baseUrl.trim();
  if (!trimmed) {
    return "";
  }

  const withoutTrailingSlash = trimmed.endsWith("/") ? trimmed.slice(0, -1) : trimmed;
  if (/\/api$/i.test(withoutTrailingSlash)) {
    return withoutTrailingSlash.replace(/\/api$/i, "");
  }

  return withoutTrailingSlash;
}

function getTunnelHeaders(baseUrl: string): Record<string, string> {
  const lower = baseUrl.toLowerCase();
  if (lower.includes("ngrok-free.app") || lower.includes("ngrok-free.dev")) {
    return { "ngrok-skip-browser-warning": "true" };
  }
  return {};
}

function buildUrl(baseUrl: string, path: string): string {
  const normalizedBase = normalizeBaseUrl(baseUrl);
  if (!normalizedBase) {
    return "";
  }

  const safePath = path.trim();
  const normalizedPath = safePath.startsWith("/") ? safePath : `/${safePath}`;
  return `${normalizedBase}${normalizedPath}`;
}

function isLikelyTunnelError(status: number, payload: unknown): boolean {
  if (status !== 404 && status !== 502) {
    return false;
  }

  if (typeof payload === "string") {
    const lower = payload.toLowerCase();
    return lower.includes("ngrok") || lower.includes("not found") || lower.includes("tunnel");
  }

  return true;
}

function getBaseCandidates(): string[] {
  const candidates = [API_BASE_URL];

  if (API_FALLBACK_URL) {
    candidates.push(API_FALLBACK_URL);
  }

  const primaryLower = API_BASE_URL.toLowerCase();
  if (primaryLower.includes("ngrok-free.app") || primaryLower.includes("ngrok-free.dev")) {
    candidates.push("http://localhost:8080", "http://127.0.0.1:8080", "http://10.0.2.2:8080");
  }

  return Array.from(new Set(candidates.map((item) => normalizeBaseUrl(item)).filter(Boolean)));
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  if (!hasApiBaseUrl()) {
    throw new ApiError("Missing API base URL. Set EXPO_PUBLIC_API_BASE_URL.", 500);
  }

  const bases = getBaseCandidates();
  const attemptedUrls: string[] = [];
  let lastApiError: ApiError | null = null;

  for (const base of bases) {
    const url = buildUrl(base, path);
    if (!url) {
      continue;
    }

    attemptedUrls.push(url);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), API_TIMEOUT_MS);

    try {
      const hasAuthorizationHeader =
        Boolean(options.headers?.Authorization) ||
        Boolean(options.headers?.authorization);

      const requestHeaders: Record<string, string> = {
        "Content-Type": "application/json",
        ...getTunnelHeaders(base),
        ...(options.headers ?? {}),
      };

      if (authToken && !hasAuthorizationHeader) {
        requestHeaders.Authorization = `Bearer ${authToken}`;
      }

      const response = await fetch(url, {
        method: options.method ?? "GET",
        headers: requestHeaders,
        body: options.body,
        signal: controller.signal,
      });

      const contentType = response.headers.get("content-type") ?? "";
      const payload = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

      if (!response.ok) {
        const message =
          typeof payload === "object" && payload && "message" in payload
            ? String((payload as { message: unknown }).message)
            : `Request failed with status ${response.status}`;

        const candidateError = new ApiError(message, response.status);
        lastApiError = candidateError;

        const canRetryWithNextBase =
          bases.length > 1 &&
          base !== bases[bases.length - 1] &&
          isLikelyTunnelError(response.status, payload);

        if (canRetryWithNextBase) {
          continue;
        }

        throw candidateError;
      }

      return payload as T;
    } catch (error) {
      if ((error as { name?: string }).name === "AbortError") {
        lastApiError = new ApiError("Request timed out. Please try again.", 408);
      } else if (error instanceof ApiError) {
        lastApiError = error;
      } else {
        lastApiError = new ApiError("Network request failed.", 503);
      }

      const shouldTryNext = base !== bases[bases.length - 1];
      if (!shouldTryNext) {
        break;
      }
    } finally {
      clearTimeout(timeout);
    }
  }

  if (lastApiError) {
    throw new ApiError(
      `${lastApiError.message} (tried: ${attemptedUrls.join(" -> ")})`,
      lastApiError.status
    );
  }

  throw new ApiError("Network request failed.", 503);
}
