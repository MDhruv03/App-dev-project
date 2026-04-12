import { USE_MOCK_SERVICES, hasApiBaseUrl } from "../config/env";
import { apiRequest } from "./apiClient";

export type VisionValidationResult = {
  faceDetected: boolean;
  faceCount: number;
  eyeDetected: boolean;
  score: number;
  hint: string;
};

export async function validateVisionFrame(image: string): Promise<VisionValidationResult> {
  const trimmed = String(image || "").trim();
  if (!trimmed) {
    throw new Error("Frame image is empty.");
  }

  if (USE_MOCK_SERVICES || !hasApiBaseUrl()) {
    throw new Error("Vision validation requires backend API connectivity.");
  }

  const payload = await apiRequest<Partial<VisionValidationResult>>("/vision/validate", {
    method: "POST",
    body: JSON.stringify({ image: trimmed }),
  });

  return {
    faceDetected: Boolean(payload.faceDetected),
    faceCount: Number(payload.faceCount ?? 0),
    eyeDetected: Boolean(payload.eyeDetected),
    score: Number(payload.score ?? 0),
    hint: String(payload.hint ?? "Vision validation updated."),
  };
}
