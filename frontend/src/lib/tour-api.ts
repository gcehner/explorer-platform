import "server-only";

import type { TourDetail } from "@/lib/tour-types";

const DEFAULT_BACKEND_URL = "http://localhost:8080";

export class TourApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "TourApiError";
  }
}

function backendUrl(): string {
  const configuredUrl = process.env.EXPLORER_BACKEND_URL ?? DEFAULT_BACKEND_URL;

  try {
    const url = new URL(configuredUrl);
    if (url.protocol !== "http:" && url.protocol !== "https:") {
      throw new Error("Unsupported protocol");
    }
    return url.toString().replace(/\/$/, "");
  } catch {
    throw new TourApiError("EXPLORER_BACKEND_URL must be a valid HTTP(S) URL.");
  }
}

export async function getManagementTour(id: number): Promise<TourDetail> {
  let response: Response;

  try {
    response = await fetch(`${backendUrl()}/api/management/tours/${id}`, {
      cache: "no-store",
      headers: { Accept: "application/json" },
    });
  } catch {
    throw new TourApiError(
      "The Explorer backend is unavailable. Start Quarkus and reload this page.",
    );
  }

  if (!response.ok) {
    throw new TourApiError(
      response.status === 404
        ? `Tour ${id} was not found.`
        : `The Explorer backend returned HTTP ${response.status}.`,
      response.status,
    );
  }

  return (await response.json()) as TourDetail;
}
