import { execFile, spawn } from "node:child_process";
import process from "node:process";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function parsePort(localAddress) {
  const raw = String(localAddress || "").trim();
  const lastColon = raw.lastIndexOf(":");
  if (lastColon < 0) {
    return null;
  }

  const portText = raw.slice(lastColon + 1).replace(/[^0-9]/g, "");
  const parsed = Number(portText);
  return Number.isInteger(parsed) ? parsed : null;
}

async function getListeningPids(port) {
  if (process.platform === "win32") {
    const { stdout } = await execFileAsync("netstat", ["-ano", "-p", "TCP"], {
      windowsHide: true,
      maxBuffer: 8 * 1024 * 1024,
    });

    const pids = new Set();
    for (const line of stdout.split(/\r?\n/)) {
      const parts = line.trim().split(/\s+/);
      if (parts.length < 5) {
        continue;
      }

      const protocol = String(parts[0] || "").toUpperCase();
      const localAddress = parts[1];
      const state = String(parts[3] || "").toUpperCase();
      const pid = Number(parts[4]);

      if (protocol !== "TCP" || state !== "LISTENING" || !Number.isInteger(pid)) {
        continue;
      }

      if (parsePort(localAddress) === port) {
        pids.add(pid);
      }
    }

    return [...pids];
  }

  try {
    const { stdout } = await execFileAsync("lsof", ["-nP", `-iTCP:${port}`, "-sTCP:LISTEN", "-t"], {
      maxBuffer: 1024 * 1024,
    });

    return [...new Set(stdout.split(/\r?\n/).map((value) => Number(value.trim())).filter(Number.isInteger))];
  } catch {
    return [];
  }
}

async function killProcessTree(pid) {
  if (!Number.isInteger(pid) || pid <= 0 || pid === process.pid) {
    return;
  }

  if (process.platform === "win32") {
    try {
      await execFileAsync("taskkill", ["/PID", String(pid), "/T", "/F"], {
        windowsHide: true,
      });
      return;
    } catch (error) {
      const output = `${error?.stdout || ""}\n${error?.stderr || ""}`.toLowerCase();
      if (output.includes("not found") || output.includes("no running instance")) {
        return;
      }
      throw error;
    }
  }

  try {
    process.kill(pid, "SIGTERM");
  } catch {
    // Ignore if already gone.
  }
}

async function ensurePortReleased(port, options = {}) {
  const { excludePid = null, maxPasses = 8 } = options;

  for (let pass = 0; pass < maxPasses; pass += 1) {
    const pids = (await getListeningPids(port)).filter((pid) => pid !== excludePid && pid !== process.pid);
    if (pids.length === 0) {
      return true;
    }

    for (const pid of pids) {
      await killProcessTree(pid);
    }

    await sleep(180);
  }

  const remaining = (await getListeningPids(port)).filter((pid) => pid !== excludePid && pid !== process.pid);
  return remaining.length === 0;
}

function parseModeAndPort() {
  const mode = String(process.argv[2] || "dev").trim().toLowerCase();
  const parsedPort = Number(process.argv[3] || process.env.PORT || "8080");
  const port = Number.isInteger(parsedPort) && parsedPort > 0 ? parsedPort : 8080;
  return { mode, port };
}

async function runStopMode(port) {
  const freed = await ensurePortReleased(port);
  if (freed) {
    console.log(`[backend-manager] Port ${port} is free.`);
    return;
  }

  console.error(`[backend-manager] Could not fully release port ${port}.`);
  process.exit(1);
}

async function runDevMode(port) {
  const preFreed = await ensurePortReleased(port);
  if (!preFreed) {
    console.error(`[backend-manager] Port ${port} is still busy before start.`);
    process.exit(1);
  }

  const child = spawn(process.execPath, ["--watch", "src/index.js"], {
    cwd: process.cwd(),
    stdio: "inherit",
    windowsHide: false,
  });

  let shuttingDown = false;

  const shutdown = async (reason) => {
    if (shuttingDown) {
      return;
    }
    shuttingDown = true;

    if (reason) {
      console.log(`[backend-manager] ${reason}`);
    }

    if (Number.isInteger(child.pid)) {
      await killProcessTree(child.pid);
    }

    await ensurePortReleased(port);
    process.exit(0);
  };

  process.on("SIGINT", () => {
    void shutdown("Stopping backend and cleaning up port...");
  });

  process.on("SIGTERM", () => {
    void shutdown("Received SIGTERM; cleaning up backend process...");
  });

  child.on("exit", async (code, signal) => {
    if (shuttingDown) {
      return;
    }

    const freed = await ensurePortReleased(port);
    if (!freed) {
      console.error(`[backend-manager] Backend exited but port ${port} is still occupied.`);
      process.exit(1);
      return;
    }

    if (signal) {
      console.log(`[backend-manager] Backend exited via signal ${signal}.`);
      process.exit(0);
      return;
    }

    process.exit(typeof code === "number" ? code : 0);
  });

  child.on("error", async (error) => {
    if (!shuttingDown) {
      console.error(`[backend-manager] Failed to launch backend: ${error instanceof Error ? error.message : String(error)}`);
      await ensurePortReleased(port);
      process.exit(1);
    }
  });
}

async function main() {
  const { mode, port } = parseModeAndPort();

  if (mode === "stop") {
    await runStopMode(port);
    return;
  }

  if (mode !== "dev") {
    console.error('[backend-manager] Unsupported mode. Use "dev" or "stop".');
    process.exit(1);
  }

  await runDevMode(port);
}

main().catch((error) => {
  console.error(`[backend-manager] ${error instanceof Error ? error.message : String(error)}`);
  process.exit(1);
});
