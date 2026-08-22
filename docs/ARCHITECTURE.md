# Architecture

Explorer Platform consists of three applications.

```text
CLI
 │
 │ REST
 ▼
Quarkus Backend
 │
 ├── PostgreSQL
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

---

## Frontend

Next.js is used for the public website.

It is responsible for:

* tour pages
* interactive map
* filtering
* search
* image gallery
* SEO

The frontend never contains business logic.

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
* generating normalized route geometry
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

GPX-derived data, including statistics, elevation profiles and route geometry, is calculated or extracted by the CLI. The CLI sends normalized import data to the backend, which validates and persists it.
