import { API_BASE_URL, API_TIMEOUT_MS, hasApiBaseUrl } from "../config/env";

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

function getTunnelHeaders(): Record<string, string> {
  const lower = API_BASE_URL.toLowerCase();
  if (lower.includes("ngrok-free.app") || lower.includes("ngrok-free.dev")) {
    return { "ngrok-skip-browser-warning": "true" };
  }
  return {};
}

function buildUrl(path: string): string {
  const normalizedBase = API_BASE_URL.endsWith("/")
    ? API_BASE_URL.slice(0, -1)
    : API_BASE_URL;
  const trimmed = path.startsWith("/") ? path : `/${path}`;
  return `${normalizedBase}${trimmed}`;
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {}
): Promise<T> {
  if (!hasApiBaseUrl()) {
    throw new ApiError("Missing API base URL. Set EXPO_PUBLIC_API_BASE_URL.", 500);
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), API_TIMEOUT_MS);

  try {
    const response = await fetch(buildUrl(path), {
      method: options.method ?? "GET",
      headers: {
        "Content-Type": "application/json",
        ...getTunnelHeaders(),
        ...(options.headers ?? {}),
      },
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
      throw new ApiError(message, response.status);
    }

    return payload as T;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    if ((error as { name?: string }).name === "AbortError") {
      throw new ApiError("Request timed out. Please try again.", 408);
    }

    throw new ApiError("Network request failed.", 503);
  } finally {
    clearTimeout(timeout);
  }
}
