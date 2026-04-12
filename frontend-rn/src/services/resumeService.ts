import { USE_MOCK_SERVICES, hasApiBaseUrl } from "../config/env";
import { apiRequest } from "./apiClient";

export type ParseResumeUploadInput = {
  fileName: string;
  mimeType: string;
  fileBase64: string;
};

export type ParseResumeUploadResult = {
  text: string;
};

export async function parseUploadedResume(input: ParseResumeUploadInput): Promise<ParseResumeUploadResult> {
  if (USE_MOCK_SERVICES || !hasApiBaseUrl()) {
    throw new Error("Resume upload parsing requires backend API connectivity.");
  }

  return apiRequest<ParseResumeUploadResult>("/profile/parse-resume", {
    method: "POST",
    body: JSON.stringify(input),
  });
}
