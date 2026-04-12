import fs from "fs";
import path from "path";
import dotenv from "dotenv";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const srcDir = path.dirname(__filename);
const backendRoot = path.resolve(srcDir, "..");
const repoRoot = path.resolve(backendRoot, "..");

const sharedEnvPath = path.resolve(repoRoot, "frontend-rn", ".env");
const backendEnvPath = path.resolve(backendRoot, ".env");
const envFilePath = fs.existsSync(sharedEnvPath) ? sharedEnvPath : backendEnvPath;

dotenv.config({ path: envFilePath });

function parseBoolean(value, fallback = false) {
  if (value === undefined || value === null || value === "") {
    return fallback;
  }
  const normalized = String(value).trim().toLowerCase();
  if (normalized === "true") return true;
  if (normalized === "false") return false;
  return fallback;
}

export const config = {
  port: Number(process.env.PORT || 8080),
  corsOrigin: process.env.CORS_ORIGIN || "*",
  groqApiKey: process.env.GROQ_API_KEY || "",
  groqModel: process.env.GROQ_MODEL || "llama-3.1-8b-instant",
  pollyEnabled: parseBoolean(process.env.POLLY_ENABLED ?? process.env.EXPO_PUBLIC_POLLY_ENABLED ?? "false", false),
  pollyProxyUrl: process.env.POLLY_PROXY_URL || process.env.EXPO_PUBLIC_POLLY_PROXY_URL || "",
  pollyVoiceId: process.env.POLLY_VOICE_ID || process.env.EXPO_PUBLIC_POLLY_VOICE_ID || "Aditi",
  dataFilePath: path.resolve(backendRoot, "data", "store.json"),
  envFilePath
};
