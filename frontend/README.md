# Explorer Frontend

The Explorer frontend uses Next.js App Router, React, TypeScript, Tailwind CSS and MapLibre GL JS.

The local management preview supports any tour through the dynamic route:

```text
http://localhost:3000/management/tours/{id}
```

The preview is deliberately disabled when `NODE_ENV=production`. The management API is currently unauthenticated and must not be exposed publicly.

## Local prerequisites

- Node.js and npm
- Explorer's Quarkus backend running locally
- A tour with a positive integer ID available from `GET http://localhost:8080/api/management/tours/{id}`

The backend URL uses the server-only `EXPLORER_BACKEND_URL` variable. Do not rename it with a `NEXT_PUBLIC_` prefix.

## Public MTB discovery

The public `/mtb` page loads published tour summaries through the narrow same-origin
`GET /api/tours` route handler. The handler forwards only the documented public-list query
parameters to Quarkus; it does not proxy `/api/management`.

The first valid bounds reported by the MapLibre map are serialized as
`west,south,east,north` and remain the fixed geographic scope for list requests during Phase 4A.
Changing the map viewport does not refresh the list yet. Country, technical difficulty, distance
and ascent filters use the corresponding backend parameters. Results use the backend's opaque
cursor and a page size of 24.

Current limitations:

- area or mountain-range filtering is not supported by the public API and remains disabled;
- cards are not links until the public `/mtb/[id]` detail route exists;
- map clusters, route geometry and viewport-driven refresh remain Phase 4B work.

The homepage `NOVE TURE` section separately requests
`GET /api/tours/latest?limit=6`. These results cover the entire published catalog and never depend
on the homepage map viewport. Homepage statistics remain intentionally unavailable because the
latest six tours are not global aggregates.

## Start on Windows CMD

From the repository root:

```cmd
cd /d C:\Projects\explorer-platform\frontend
copy .env.example .env.local
npm install
npm run dev
```

Then open `http://localhost:3000/management/tours/{id}`, replacing `{id}` with the ID of an available tour.

To use a different local backend, edit `EXPLORER_BACKEND_URL` in `.env.local`. The browser never receives this value; the Next.js server fetches the tour and passes only the response data to the page.

## Development basemap

Local development uses the OpenFreeMap Liberty style by default:

```text
https://tiles.openfreemap.org/styles/liberty
```

It is an OpenStreetMap-based vector basemap rendered with MapLibre GL JS. MapLibre's attribution control remains enabled so the attribution included by the style is displayed. OpenFreeMap is a development default, not a production tile-provider commitment.

Override the MapLibre-compatible style URL in `.env.local` when needed:

```text
NEXT_PUBLIC_MAP_STYLE_URL=https://tiles.openfreemap.org/styles/liberty
```

Review the provider's current usage terms before deployment: https://openfreemap.org/tos/

## Verification

```cmd
npm run lint
npm test
npm run typecheck
npm run build
```

The production build intentionally serves a 404 for every `/management/tours/{id}` preview until authentication is implemented.
