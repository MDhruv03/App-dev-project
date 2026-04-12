import crypto from "crypto";
import { promisify } from "util";

const scryptAsync = promisify(crypto.scrypt);

export function normalizeEmail(value) {
  return String(value || "").trim().toLowerCase();
}

export function sanitizeDisplayName(value) {
  const name = String(value || "").trim();
  return name || "Learner";
}

export function isValidEmail(value) {
  const email = normalizeEmail(value);
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export function isStrongPassword(value) {
  const password = String(value || "");
  return password.length >= 8;
}

export async function hashPassword(password) {
  const plain = String(password || "");
  const salt = crypto.randomBytes(16).toString("hex");
  const derived = await scryptAsync(plain, salt, 64);
  return `${salt}:${Buffer.from(derived).toString("hex")}`;
}

export async function verifyPassword(password, storedHash) {
  const plain = String(password || "");
  const raw = String(storedHash || "");
  const [salt, expected] = raw.split(":");

  if (!salt || !expected) {
    return false;
  }

  const derived = await scryptAsync(plain, salt, 64);
  const actual = Buffer.from(derived).toString("hex");

  const expectedBuffer = Buffer.from(expected, "hex");
  const actualBuffer = Buffer.from(actual, "hex");

  if (expectedBuffer.length !== actualBuffer.length) {
    return false;
  }

  return crypto.timingSafeEqual(expectedBuffer, actualBuffer);
}

export function createSessionToken() {
  return crypto.randomBytes(32).toString("hex");
}
