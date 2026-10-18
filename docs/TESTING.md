# Testing

## Backend integration-test database

Backend integration tests use a dedicated PostgreSQL/PostGIS database named `explorer_test`.
It runs in a separate container on port `5433`. The development database `explorer` remains on
port `5432` and is not used by tests.

Start and initialize the test database from the repository root:

```text
docker compose up -d --wait postgres-test
```

The first startup creates `explorer_test`. Quarkus runs the existing Flyway migrations against it
when the backend test suite starts. The test database is stored in its own Docker volume, so it does
not need to be recreated for each run.

Run the backend tests:

```text
cd backend
./mvnw test
```

On Windows PowerShell, use `./mvnw.cmd test`.

Test datasource configuration is in `backend/src/test/resources/application.properties`. Quarkus
Dev Services is disabled for tests, so an unavailable `explorer_test` database causes startup to
fail instead of silently selecting another database.

Before every `@QuarkusTest` method, a safety callback checks both that the resolved JDBC URL names
`explorer_test` and that PostgreSQL's `current_database()` returns `explorer_test`. The test run is
stopped before fixture cleanup if either check fails.
