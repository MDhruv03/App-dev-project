import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const envPath = path.resolve(scriptDir, "..", ".env");

function normalizeAddr(value) {
  return String(value || "")
    .trim()
    .replace(/^https?:\/\//i, "")
    .replace(/\/+$/, "")
    .toLowerCase();
}

const targetBackendAddr = "localhost:8080";
const ngrokApiPorts = [4040, 4041];

function isHttpUrl(value) {
  return /^https?:\/\//i.test(String(value || "").trim());
}

async function readExistingApiBaseUrl() {
  try {
    const content = await readFile(envPath, "utf8");
    const match = content.match(/^EXPO_PUBLIC_API_BASE_URL=(.*)$/m);
    return String(match?.[1] || "").trim();
  } catch {
    return "";
  }
}

async function requestNgrok(path, init = {}) {
  const errors = [];

  for (const port of ngrokApiPorts) {
    const url = `http://127.0.0.1:${port}${path}`;
    try {
      const response = await fetch(url, init);
      if (!response.ok) {
        const body = await response.text().catch(() => "");
        errors.push(`${url} -> HTTP ${response.status} ${body}`);
        continue;
      }

      if (response.status === 204) {
        return null;
      }

      return await response.json();
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      errors.push(`${url} -> ${message}`);
    }
  }

  throw new Error(errors.join(" | "));
}

async function getTunnels() {
  const payload = await requestNgrok("/api/tunnels");
  return Array.isArray(payload?.tunnels) ? payload.tunnels : [];
}

function findBackendTunnel(tunnels) {
  const matched = tunnels.filter(
    (tunnel) => normalizeAddr(tunnel?.config?.addr) === targetBackendAddr
  );

  const httpsFirst = matched.find((tunnel) =>
    String(tunnel?.public_url || "").toLowerCase().startsWith("https://")
  );

  return httpsFirst ?? matched[0] ?? null;
}

function deriveExpHostname(tunnels) {
  for (const tunnel of tunnels) {
    const raw = String(tunnel?.public_url || "").trim();
    if (!raw) {
      continue;
    }

    let host = "";
    try {
      host = new URL(raw).hostname;
    } catch {
      continue;
    }

    const match = host.match(/^(.*)-\d+\.exp\.direct$/i);
    if (match && match[1]) {
      return `${match[1]}-8080.exp.direct`;
    }
  }

  return "";
}

async function createBackendTunnel(hostname) {
  const payload = {
    name: "backend-8080",
    addr: "http://localhost:8080",
    proto: "http",
    hostname,
  };

  return requestNgrok("/api/tunnels", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

async function main() {
  let tunnels = [];
  try {
    tunnels = await getTunnels();
  } catch (error) {
    const existingUrl = await readExistingApiBaseUrl();
    if (isHttpUrl(existingUrl)) {
      process.stdout.write(
        `ngrok local API unavailable; keeping existing EXPO_PUBLIC_API_BASE_URL: ${existingUrl}\n`
      );
      return;
    }
    throw error;
  }

  const existing = findBackendTunnel(tunnels);

  if (existing) {
    process.stdout.write(`Backend tunnel already available: ${existing.public_url}\n`);
    return;
  }

  const hostname = deriveExpHostname(tunnels);
  if (!hostname) {
    const existingUrl = await readExistingApiBaseUrl();
    if (isHttpUrl(existingUrl)) {
      process.stdout.write(
        `No Expo tunnel context found; keeping existing EXPO_PUBLIC_API_BASE_URL: ${existingUrl}\n`
      );
      return;
    }

    throw new Error(
      "No exp.direct tunnel context found. Start Expo with tunnel first, then rerun this command."
    );
  }

  const created = await createBackendTunnel(hostname);
  const publicUrl = String(created?.public_url || "").trim();
  if (!publicUrl) {
    throw new Error("Tunnel creation response did not include public_url.");
  }

  process.stdout.write(`Created backend tunnel: ${publicUrl}\n`);
}

main().catch((error) => {
  const message = error instanceof Error ? error.message : "Unknown error";
  process.stderr.write(`open-backend-tunnel failed: ${message}\n`);
  process.exit(1);
});
