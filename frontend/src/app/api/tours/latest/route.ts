import { getBackendBaseUrl } from "@/lib/tour-api";

export async function GET(request: Request) {
  const incoming = new URL(request.url);
  const upstreamQuery = new URLSearchParams();
  const limit = incoming.searchParams.get("limit");
  if (limit !== null) upstreamQuery.set("limit", limit);

  try {
    const query = upstreamQuery.size > 0 ? `?${upstreamQuery.toString()}` : "";
    const upstream = await fetch(
      `${getBackendBaseUrl()}/api/tours/latest${query}`,
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
      { message: "Najnovejših tur trenutno ni mogoče naložiti." },
      { status: 502, headers: { "Cache-Control": "no-store" } },
    );
  }
}
