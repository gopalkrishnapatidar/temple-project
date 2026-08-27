import type { PingResponse, PingResult } from "./types";

const DEFAULT_API_BASE_URL = "http://localhost:8080";

export function getApiBaseUrl(): string {
  const configuredBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL?.trim();

  if (!configuredBaseUrl) {
    return DEFAULT_API_BASE_URL;
  }

  return configuredBaseUrl.replace(/\/$/, "");
}

export async function fetchPing(): Promise<PingResult> {
  const url = `${getApiBaseUrl()}/api/v1/system/ping`;

  try {
    const response = await fetch(url, { cache: "no-store" });

    if (!response.ok) {
      return {
        ok: false,
        error: "Backend service is temporarily unavailable.",
      };
    }

    const data = (await response.json()) as PingResponse;

    return { ok: true, data };
  } catch {
    return {
      ok: false,
      error: "Backend service is temporarily unavailable.",
    };
  }
}
