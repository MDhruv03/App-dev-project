import { File, Paths } from "expo-file-system";
import {
  API_TIMEOUT_MS,
  POLLY_ENABLED,
  POLLY_ENGINE,
  POLLY_PROXY_URL,
  POLLY_VOICE_ID,
} from "../config/env";

type PollyAttempt = {
  url: string;
  voiceId: string;
  engine: string;
};

type PollyPayload = {
  base64Audio: string;
  contentType: string;
};

export type PollyPlaybackAsset = {
  fileUri: string;
  voiceId: string;
  engine: string;
  cleanup: () => Promise<void>;
};

const BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

function removeWhitespace(value: string): string {
  return value.replace(/\s+/g, "").trim();
}

function normalizeBase64(input: string): string {
  if (!input) return "";
  const strippedPrefix = input.replace(/^data:[^;]+;base64,/i, "");
  return removeWhitespace(strippedPrefix);
}

function isLikelyBase64(value: string): boolean {
  return /^[A-Za-z0-9+/=]+$/.test(value) && value.length >= 64;
}

function looksLikeMp3(bytes: Uint8Array): boolean {
  if (bytes.length < 3) return false;
  const hasId3 = bytes[0] === 0x49 && bytes[1] === 0x44 && bytes[2] === 0x33;
  const hasFrameSync = bytes[0] === 0xff && (bytes[1] & 0xe0) === 0xe0;
  return hasId3 || hasFrameSync;
}

function bytesToBase64(bytes: Uint8Array): string {
  let result = "";
  for (let index = 0; index < bytes.length; index += 3) {
    const byte1 = bytes[index] ?? 0;
    const byte2 = bytes[index + 1] ?? 0;
    const byte3 = bytes[index + 2] ?? 0;

    const triplet = (byte1 << 16) | (byte2 << 8) | byte3;

    result += BASE64_ALPHABET[(triplet >> 18) & 0x3f];
    result += BASE64_ALPHABET[(triplet >> 12) & 0x3f];
    result += index + 1 < bytes.length ? BASE64_ALPHABET[(triplet >> 6) & 0x3f] : "=";
    result += index + 2 < bytes.length ? BASE64_ALPHABET[triplet & 0x3f] : "=";
  }

  return result;
}

function alternateProxyUrl(url: string): string | null {
  if (url.endsWith("/polly")) {
    return `${url.slice(0, -"/polly".length)}/polly-proxy`;
  }
  if (url.endsWith("/polly-proxy")) {
    return `${url.slice(0, -"/polly-proxy".length)}/polly`;
  }
  return null;
}

function buildAttempts(): PollyAttempt[] {
  const normalizedUrl = POLLY_PROXY_URL.trim();
  const preferredVoice = POLLY_VOICE_ID.trim() || "Gregory";
  const preferredEngine = POLLY_ENGINE.trim() || "neural";

  const urls = [normalizedUrl];
  const alternate = alternateProxyUrl(normalizedUrl);
  if (alternate) {
    urls.push(alternate);
  }

  const voices = Array.from(new Set([preferredVoice, "Aditi", "Matthew"]));
  const engines = Array.from(new Set([preferredEngine, "neural", "standard"]));

  const attempts: PollyAttempt[] = [];
  for (const url of urls) {
    for (const voiceId of voices) {
      for (const engine of engines) {
        attempts.push({ url, voiceId, engine });
      }
    }
  }

  return attempts;
}

async function fetchAudioUrlAsBase64(audioUrl: string): Promise<PollyPayload | null> {
  try {
    const response = await fetch(audioUrl);
    if (!response.ok) {
      return null;
    }

    const bytes = new Uint8Array(await response.arrayBuffer());
    if (bytes.length === 0) {
      return null;
    }

    return {
      base64Audio: bytesToBase64(bytes),
      contentType: response.headers.get("content-type") ?? "audio/mpeg",
    };
  } catch {
    return null;
  }
}

async function parsePayloadFromJson(raw: string): Promise<PollyPayload | null> {
  const trimmed = raw.trim();
  if (!trimmed.startsWith("{")) {
    return null;
  }

  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(trimmed) as Record<string, unknown>;
  } catch {
    return null;
  }

  const audioBase64 = typeof parsed.audioBase64 === "string" ? parsed.audioBase64 : "";
  if (audioBase64.trim()) {
    return {
      base64Audio: normalizeBase64(audioBase64),
      contentType: typeof parsed.contentType === "string" ? parsed.contentType : "audio/mpeg",
    };
  }

  const lambdaBody = typeof parsed.body === "string" ? parsed.body : "";
  const lambdaBase64 = Boolean(parsed.isBase64Encoded);
  if (lambdaBody.trim() && lambdaBase64) {
    return {
      base64Audio: normalizeBase64(lambdaBody),
      contentType: "audio/mpeg",
    };
  }

  const audioUrl = typeof parsed.audioUrl === "string" ? parsed.audioUrl.trim() : "";
  if (audioUrl) {
    return fetchAudioUrlAsBase64(audioUrl);
  }

  return null;
}

async function decodePollyResponse(response: Response): Promise<PollyPayload> {
  const contentType = response.headers.get("content-type") ?? "";
  const textBody = await response.clone().text().catch(() => "");

  const jsonPayload = await parsePayloadFromJson(textBody);
  if (jsonPayload && jsonPayload.base64Audio) {
    return jsonPayload;
  }

  const normalizedText = normalizeBase64(textBody);
  if (isLikelyBase64(normalizedText)) {
    return {
      base64Audio: normalizedText,
      contentType: contentType || "audio/mpeg",
    };
  }

  const bytes = new Uint8Array(await response.arrayBuffer());
  if (bytes.length === 0) {
    throw new Error("Polly returned empty audio.");
  }

  const binaryBase64 = bytesToBase64(bytes);
  if (!binaryBase64) {
    throw new Error("Failed to decode Polly audio payload.");
  }

  if (contentType.toLowerCase().includes("audio") || looksLikeMp3(bytes)) {
    return {
      base64Audio: binaryBase64,
      contentType: contentType || "audio/mpeg",
    };
  }

  throw new Error("Unsupported Polly response format.");
}

async function requestPollyAudio(attempt: PollyAttempt, text: string): Promise<PollyPayload> {
  const timeoutMs = Math.max(API_TIMEOUT_MS, 18000);
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(attempt.url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        text,
        voiceId: attempt.voiceId,
        engine: attempt.engine,
        outputFormat: "mp3",
      }),
      signal: controller.signal,
    });

    if (!response.ok) {
      const bodyText = (await response.text().catch(() => "")).trim();
      const detail = bodyText ? bodyText.slice(0, 180) : `HTTP ${response.status}`;
      throw new Error(detail);
    }

    return await decodePollyResponse(response);
  } catch (error) {
    if ((error as { name?: string }).name === "AbortError") {
      throw new Error("request timed out");
    }

    if (error instanceof Error) {
      throw error;
    }

    throw new Error("unknown Polly request failure");
  } finally {
    clearTimeout(timeout);
  }
}

async function persistAudio(base64Audio: string): Promise<{ fileUri: string; cleanup: () => Promise<void> }> {
  const normalized = normalizeBase64(base64Audio);
  if (!normalized || !isLikelyBase64(normalized)) {
    throw new Error("Polly audio payload is not valid base64.");
  }

  const file = new File(Paths.cache, `polly-${Date.now()}-${Math.random().toString(36).slice(2, 8)}.mp3`);
  file.create();
  file.write(normalized, { encoding: "base64" });

  return {
    fileUri: file.uri,
    cleanup: async () => {
      try {
        if (file.exists) {
          file.delete();
        }
      } catch {
        // Cleanup is best effort only.
      }
    },
  };
}

export function getPollyDisabledReason(): string | null {
  if (!POLLY_ENABLED) {
    return "EXPO_PUBLIC_POLLY_ENABLED is false";
  }

  const proxyUrl = POLLY_PROXY_URL.trim();
  if (!proxyUrl) {
    return "EXPO_PUBLIC_POLLY_PROXY_URL is missing";
  }

  const lower = proxyUrl.toLowerCase();
  if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
    return "Polly URL must start with http:// or https://";
  }

  if (proxyUrl.includes("your-") || proxyUrl.includes("example.com")) {
    return "Polly URL still looks like a placeholder";
  }

  return null;
}

export function isPollyConfigured(): boolean {
  return getPollyDisabledReason() === null;
}

export async function synthesizePollySpeech(text: string): Promise<PollyPlaybackAsset> {
  const reason = getPollyDisabledReason();
  if (reason) {
    throw new Error(reason);
  }

  const spokenText = text.trim();
  if (!spokenText) {
    throw new Error("No text provided for interviewer voice.");
  }

  const attempts = buildAttempts();
  let lastError: string | null = null;

  for (const attempt of attempts) {
    try {
      const payload = await requestPollyAudio(attempt, spokenText);
      const persisted = await persistAudio(payload.base64Audio);
      return {
        fileUri: persisted.fileUri,
        voiceId: attempt.voiceId,
        engine: attempt.engine,
        cleanup: persisted.cleanup,
      };
    } catch (error) {
      const detail = error instanceof Error ? error.message : "unknown";
      lastError = `voice=${attempt.voiceId}, engine=${attempt.engine}: ${detail}`;
    }
  }

  throw new Error(lastError ?? "Polly synthesis failed.");
}
