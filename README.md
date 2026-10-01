# Explorer Platform

Explorer Platform is a personal outdoor platform for documenting, organizing and sharing MTB and ski touring adventures.

The project is built around:

* GPX tracks
* maps
* photos
* tour descriptions
* Garmin Connect imports
* public tour pages

## Applications

```text
backend/   Quarkus REST API
frontend/  Next.js web application
cli/       Java command-line tool
docs/      project documentation
storage/   local development storage
```

## Goal

The goal is to build a simple, extensible platform that starts as a personal project, but can later support an admin panel, multiple users and additional import sources.

## Status

The backend supports creating DRAFT MTB tours from normalized GPX imports. The Java CLI parses the
source GPX, calculates the normalized route data and uploads both that data and the unchanged source
file to the backend.

## CLI GPX import

The CLI requires Java 21. Build it with:

```text
cd cli
mvn package
```

Import one GPX track into a locally running backend:

```text
java -jar target/explorer.jar tour import path/to/track.gpx
```

Use `--backend-url` to select another backend. If the option is omitted, the CLI uses
`EXPLORER_BACKEND_URL`, then falls back to `http://localhost:8080`.
