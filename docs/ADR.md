# Architecture Decisions

## API First

The CLI, frontend and future admin panel communicate with the backend through APIs.

Only the backend accesses the database.

Status: Accepted

---

## Backend Owns the Domain

Business rules, validation, GPX processing and derived data generation belong to the backend.

Clients are responsible only for user interaction and data transfer.

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

## Tours Have Owners

Every tour belongs to a user, even while the platform initially supports only one user.

This allows future multi-user support without restructuring the core domain.

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
