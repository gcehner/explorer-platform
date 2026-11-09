import { getBackendBaseUrl } from "@/lib/tour-api";

const PUBLIC_LIST_PARAMETERS = [
  "bbox",
  "countryCode",
  "technicalDifficulty",
  "distanceMinKm",
  "distanceMaxKm",
  "ascentMinMeters",
  "ascentMaxMeters",
  "pageSize",
  "cursor",
] as const;

export async function GET(request: Request) {
  const incoming = new URL(request.url);
  const upstreamQuery = new URLSearchParams();

  for (const parameter of PUBLIC_LIST_PARAMETERS) {
    for (const value of incoming.searchParams.getAll(parameter)) {
      upstreamQuery.append(parameter, value);
    }
  }

  try {
    const upstream = await fetch(
      `${getBackendBaseUrl()}/api/tours?${upstreamQuery.toString()}`,
      {
        cache: "no-store",
        headers: { Accept: "application/json" },
        signal: request.signal,
      },
    );

    return new Response(upstream.body, {
      status: upstream.status,
      headers: {
        "Cache-Control": "no-store",
        "Content-Type": upstream.headers.get("Content-Type") ?? "application/json",
      },
    });
  } catch {
    return Response.json(
      { message: "Javni seznam tur trenutno ni dosegljiv." },
      { status: 502, headers: { "Cache-Control": "no-store" } },
    );
  }
}
