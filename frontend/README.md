# Explorer Frontend

The Explorer frontend uses Next.js App Router, React, TypeScript, Tailwind CSS and MapLibre GL JS.

The first milestone is a local-only preview of any DRAFT tour at the dynamic route:

```text
http://localhost:3000/management/tours/{id}
```

Tour 34 is the current development example:

```text
http://localhost:3000/management/tours/34
```

The preview is deliberately disabled when `NODE_ENV=production`. The management API is currently unauthenticated and must not be exposed publicly.

## Local prerequisites

- Node.js and npm
- Explorer's Quarkus backend running locally
- A tour with a positive integer ID available from `GET http://localhost:8080/api/management/tours/{id}` (Tour 34 is the development example)

The backend URL uses the server-only `EXPLORER_BACKEND_URL` variable. Do not rename it with a `NEXT_PUBLIC_` prefix.

## Start on Windows CMD

From the repository root:

```cmd
cd /d C:\Projects\explorer-platform\frontend
copy .env.example .env.local
npm install
npm run dev
```

Then open `http://localhost:3000/management/tours/34` for the development example, or replace `34` with another available tour ID.

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
npm run typecheck
npm run build
```

The production build intentionally serves a 404 for every `/management/tours/{id}` preview until authentication is implemented.
