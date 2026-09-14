# Architecture Decisions

## Technology Stack

The backend is implemented using Quarkus.

Reasons

- Familiar Jakarta EE programming model
- CDI
- Jakarta REST
- Hibernate ORM
- Excellent Java 21 support
- Lightweight runtime

## Backend Framework

The backend uses Quarkus and follows Jakarta EE standards where possible.

## Jakarta First

The backend is implemented using Quarkus.

Application code follows Jakarta EE standards whenever possible.

Quarkus-specific features are used only when they provide clear benefits without introducing unnecessary coupling.

## API First

The CLI, frontend and future admin panel communicate with the backend through APIs.

Only the backend accesses the database.

Status: Accepted

---

## Backend Owns the Persisted Domain

Business rules, validation, persistence and the Tour publication lifecycle belong to the backend.

GPX parsing and the calculation or extraction of GPX-derived data are the responsibility of the CLI. This includes route statistics, the tour date when timestamps are available, the elevation profile and route geometry.

The backend receives normalized import data, validates it and persists it.

Status: Accepted

---

## CLI Before Admin Panel

The first content management interface is a Java CLI.

An admin panel may be added later without changing the backend domain model or import APIs.

Status: Accepted

---

## Garmin Integration Lives in the CLI

The CLI authenticates with Garmin Connect and downloads activities.

The backend accepts imported files and provider metadata without depending directly on Garmin Connect.

Status: Accepted

---

## Original GPX Files Are Immutable

The original imported GPX file is stored without modification.

Cleaned GPX files, statistics, elevation profiles and simplified geometries are derived artifacts that can be regenerated.

Status: Accepted

---

## Files Are Stored Outside the Database

GPX files and images are stored in file or object storage.

The database contains metadata and storage references.

Status: Accepted

---

## V1 Has No Tour Ownership

Explorer V1 is single-user and authentication is not part of the current scope.

Tours therefore have no User entity or owner relationship. Ownership will be designed only when
multi-user requirements are defined.

Status: Accepted

---

## Route Geometry Uses PostGIS

The CLI generates normalized route geometry as a GeoJSON LineString using `[longitude, latitude]`
coordinate order. GeoJSON is an API representation only.

The backend validates the geometry and persists it once as `geometry(LineString, 4326)` in PostGIS.
The original immutable GPX is retained separately in file storage. The backend does not parse GPX.

Status: Accepted

---

## Public and Management APIs Are Separated

Public read operations and authenticated management operations use separate API namespaces.

Example:

```text
/api/public/tours
/api/management/tours
/api/management/imports
```

Status: Accepted
