import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const envPath = path.resolve(scriptDir, "..", ".env");
const ngrokApiPorts = [4040, 4041];
const targetAddrs = new Set([
  "localhost:8080",
  "127.0.0.1:8080",
  "0.0.0.0:8080",
  "[::1]:8080",
]);

function normalizeAddr(value) {
  return String(value || "")
    .trim()
    .replace(/^https?:\/\//i, "")
    .replace(/\/+$/, "")
    .toLowerCase();
}

function upsertEnvLine(content, key, value, eol) {
  const line = `${key}=${value}`;
  const pattern = new RegExp(`^${key}=.*$`, "m");

  if (pattern.test(content)) {
    return content.replace(pattern, line);
  }

  const suffix = content.endsWith(eol) ? "" : eol;
  return `${content}${suffix}${line}${eol}`;
}

function readEnvValue(content, key) {
  const pattern = new RegExp(`^${key}=(.*)$`, "m");
  const match = content.match(pattern);
  return String(match?.[1] || "").trim();
}

function isHttpUrl(value) {
  return /^https?:\/\//i.test(String(value || "").trim());
}

async function readNgrokTunnels() {
  const errors = [];
  const aggregated = [];

  for (const port of ngrokApiPorts) {
    try {
      const response = await fetch(`http://127.0.0.1:${port}/api/tunnels`);
      if (!response.ok) {
        errors.push(`:${port} -> HTTP ${response.status}`);
        continue;
      }

      const payload = await response.json();
      const tunnels = Array.isArray(payload?.tunnels) ? payload.tunnels : [];
      aggregated.push(...tunnels);
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      errors.push(`:${port} -> ${message}`);
    }
  }

  if (aggregated.length > 0) {
    const seen = new Set();
    return aggregated.filter((tunnel) => {
      const key = `${String(tunnel?.public_url || "")}::${String(tunnel?.config?.addr || "")}`;
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
  }

  throw new Error(
    `Cannot reach ngrok local API on ports 4040/4041 (${errors.join(" | ")}). Start ngrok first.`
  );
}

function pickBackendUrl(tunnels) {
  const backendTunnels = tunnels.filter((tunnel) => {
    const addr = normalizeAddr(tunnel?.config?.addr);
    return targetAddrs.has(addr);
  });

  if (backendTunnels.length === 0) {
    return "";
  }

  const sorted = [...backendTunnels].sort((a, b) => {
    const aUrl = String(a?.public_url || "").toLowerCase();
    const bUrl = String(b?.public_url || "").toLowerCase();

    const score = (url) => {
      if (url.startsWith("https://") && (url.includes("ngrok-free.app") || url.includes("ngrok-free.dev"))) {
        return 4;
      }
      if (url.startsWith("https://") && (url.includes("ngrok.app") || url.includes("ngrok.dev"))) {
        return 3;
      }
      if (url.startsWith("https://")) {
        return 2;
      }
      if (url.startsWith("http://")) {
        return 1;
      }
      return 0;
    };

    return score(bUrl) - score(aUrl);
  });

  const selected = sorted[0];
  return String(selected?.public_url || "").trim();
}

async function main() {
  const envRaw = await readFile(envPath, "utf8");
  const eol = envRaw.includes("\r\n") ? "\r\n" : "\n";
  const existingBaseUrl = readEnvValue(envRaw, "EXPO_PUBLIC_API_BASE_URL");

  let tunnels = [];
  let backendUrl = "";

  try {
    tunnels = await readNgrokTunnels();
    backendUrl = pickBackendUrl(tunnels);
  } catch {
    if (!isHttpUrl(existingBaseUrl)) {
      throw new Error(
        "Cannot reach ngrok local API and EXPO_PUBLIC_API_BASE_URL is missing. Start tunnel and rerun."
      );
    }

    let next = envRaw;
    next = upsertEnvLine(next, "EXPO_PUBLIC_API_FALLBACK_URL", "http://localhost:8080", eol);
    if (next !== envRaw) {
      await writeFile(envPath, next, "utf8");
    }

    process.stdout.write(
      `ngrok local API unavailable; keeping existing EXPO_PUBLIC_API_BASE_URL: ${existingBaseUrl}\n`
    );
    return;
  }

  if (!backendUrl) {
    if (isHttpUrl(existingBaseUrl)) {
      let next = envRaw;
      next = upsertEnvLine(next, "EXPO_PUBLIC_API_FALLBACK_URL", "http://localhost:8080", eol);
      if (next !== envRaw) {
        await writeFile(envPath, next, "utf8");
      }

      process.stdout.write(
        `No localhost:8080 tunnel found; keeping existing EXPO_PUBLIC_API_BASE_URL: ${existingBaseUrl}\n`
      );
      return;
    }

    const known = tunnels
      .map((tunnel) => `${String(tunnel?.public_url || "")} -> ${String(tunnel?.config?.addr || "")}`)
      .filter(Boolean)
      .join(" | ");

    throw new Error(
      `No ngrok tunnel found for localhost:8080. Start one first (npx ngrok http 8080). Known tunnels: ${known || "none"}`
    );
  }

  let next = envRaw;
  next = upsertEnvLine(next, "EXPO_PUBLIC_API_BASE_URL", backendUrl, eol);
  next = upsertEnvLine(next, "EXPO_PUBLIC_API_FALLBACK_URL", "http://localhost:8080", eol);

  if (next !== envRaw) {
    await writeFile(envPath, next, "utf8");
  }

  process.stdout.write(`Updated .env API base URL to ${backendUrl}\n`);
}

main().catch((error) => {
  const message = error instanceof Error ? error.message : "Unknown error";
  process.stderr.write(`sync-ngrok-url failed: ${message}\n`);
  process.exit(1);
});
