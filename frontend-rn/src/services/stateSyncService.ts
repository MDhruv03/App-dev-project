import { apiRequest } from "./apiClient";

type BackendBootstrap = {
  profile?: unknown;
  coding?: unknown;
  opportunities?: unknown;
  applications?: unknown;
  interview?: unknown;
  activityLog?: unknown;
};

type BackendStateSnapshot = {
  profile: unknown;
  coding: unknown;
  opportunities: unknown;
  applications: unknown;
  interview: unknown;
  activityLog: unknown;
};

export async function fetchBackendBootstrapState(): Promise<BackendBootstrap> {
  return apiRequest<BackendBootstrap>("/state/bootstrap", {
    method: "GET",
  });
}

export async function pushBackendStateSnapshot(snapshot: BackendStateSnapshot): Promise<void> {
  await apiRequest<{ ok: boolean }>("/state/save", {
    method: "POST",
    body: JSON.stringify(snapshot),
  });
}
