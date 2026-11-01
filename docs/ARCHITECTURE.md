# Architecture

Explorer Platform consists of three applications.

```text
CLI
 │
 │ REST
 ▼
Quarkus Backend
 │
 ├── PostgreSQL with PostGIS
 ├── File Storage
 └── Normalized Tour Data Persistence
 │
 ▼
Frontend
```

## Backend

The backend is implemented using Quarkus.

It owns:

- domain model
- business logic
- database access
- validation of normalized GPX import data
- persistence of route statistics
- persistence of elevation profiles and route geometry
- public API
- future management API

The backend is the single source of truth.

### API exposure

Public Tour reads use the `/api/tours` namespace. Management operations use
`/api/management`.

The management API is currently unauthenticated and must not be exposed publicly without access
control.

---

## Frontend

Next.js is used for the public website.

The approved public information architecture and navigation behavior are defined in
[NAVIGATION.md](NAVIGATION.md).

It is responsible for:

* tour pages
* interactive map
* filtering
* search
* image gallery
* SEO

The frontend never contains business logic.

### Public MTB discovery

The approved MTB listing layout is the split-view interface in
[`mockups/mtb.html`](mockups/mtb.html): a compact filter area and tour results beside an
interactive MapLibre map.

The backend provides `GET /api/tours` for geographically filtered, cursor-paginated published-tour
summaries. Geographic inclusion uses a stored point at the midpoint of each route. The endpoint and
its validation and pagination contract are documented in [API.md](API.md).

The frontend does not yet consume the collection endpoint and must never use the unauthenticated
management API. Map clustering, bounded geometry discovery, markers, filters and list/map selection
synchronization remain planned follow-up work.

---

## CLI

The CLI is the content import tool.

It is responsible for:

* Garmin Connect authentication
* downloading activities
* importing GPX files
* parsing GPX files
* calculating distance and elevation statistics
* extracting the tour date from GPX timestamps when available
* generating elevation profiles
* generating normalized route geometry as a GeoJSON LineString
* importing photos
* creating tour drafts
* publishing tours
* automation and batch operations

The CLI communicates only through the backend REST API.

It never accesses the database directly.

---

## Storage

Original GPX files and uploaded images are stored as files.

The database stores metadata and references to files.

Original GPX files are immutable.

GPX-derived data, including statistics, elevation profiles and route geometry, is calculated or extracted by the CLI. The CLI sends route geometry as a normalized GeoJSON LineString in `[longitude, latitude]` order. The backend validates it and persists it in PostGIS as `geometry(LineString, 4326)` without retaining a second GeoJSON representation.

The original GPX is retained separately in file storage. The backend stores it without parsing it and records an opaque, backend-generated storage reference.

For V1, the file is stored before the database transaction commits. A storage failure prevents the
Tour from being persisted. If database persistence subsequently fails, the stored GPX can remain as
an orphan; distributed transaction and cleanup machinery are intentionally outside this slice.
