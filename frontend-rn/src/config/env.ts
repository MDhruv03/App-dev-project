const DEFAULT_TIMEOUT_MS = 12000;

function getEnvValue(name: string): string | undefined {
  if (typeof process === "undefined" || !process.env) {
    return undefined;
  }
  return process.env[name];
}

function parseBoolean(value: string | undefined, fallback: boolean): boolean {
  if (!value) return fallback;
  if (value.toLowerCase() === "true") return true;
  if (value.toLowerCase() === "false") return false;
  return fallback;
}

function parseNumber(value: string | undefined, fallback: number): number {
  if (!value) return fallback;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export const APP_ENV = getEnvValue("EXPO_PUBLIC_APP_ENV") ?? "development";
export const API_BASE_URL = getEnvValue("EXPO_PUBLIC_API_BASE_URL")?.trim() ?? "";
export const API_TIMEOUT_MS = parseNumber(
  getEnvValue("EXPO_PUBLIC_API_TIMEOUT_MS"),
  DEFAULT_TIMEOUT_MS
);
export const USE_MOCK_SERVICES = parseBoolean(
  getEnvValue("EXPO_PUBLIC_USE_MOCKS"),
  true
);

export const POLLY_ENABLED = parseBoolean(
  getEnvValue("EXPO_PUBLIC_POLLY_ENABLED"),
  false
);
export const POLLY_PROXY_URL = getEnvValue("EXPO_PUBLIC_POLLY_PROXY_URL")?.trim() ?? "";
export const POLLY_VOICE_ID = getEnvValue("EXPO_PUBLIC_POLLY_VOICE_ID")?.trim() || "Aditi";
export const POLLY_ENGINE = getEnvValue("EXPO_PUBLIC_POLLY_ENGINE")?.trim() || "neural";

export function hasApiBaseUrl(): boolean {
  return API_BASE_URL.length > 0;
}

export function hasPollyProxyUrl(): boolean {
  return POLLY_PROXY_URL.length > 0;
}
