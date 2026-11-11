package com.gregisoft.explorer.tour.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.restassured.response.Response;

import static com.gregisoft.explorer.test.TestDatabaseSafety.assertUsingIsolatedTestDatabase;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class GpxImportResourceTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-gpx-storage");

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    void cleanState() throws IOException {
        assertUsingIsolatedTestDatabase(entityManager);
        entityManager.createQuery("delete from GpxTrack").executeUpdate();
        entityManager.createQuery("delete from Tour").executeUpdate();
        if (Files.isDirectory(STORAGE_ROOT)) {
            try (Stream<Path> paths = Files.list(STORAGE_ROOT)) {
                for (Path path : paths.toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    @Test
    void validMultipartImportReturnsCreatedDraftAndPersistsFileAndGeometry() throws IOException {
        long tourId = given()
                .multiPart("metadata", "metadata.json", validMetadata(), "application/json")
                .multiPart("gpxFile", "original.gpx", "<gpx>untouched</gpx>".getBytes(),
                        "application/gpx+xml")
                .when()
                .post("/api/management/imports/gpx")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("status", equalTo("DRAFT"))
                .extract()
                .jsonPath()
                .getLong("tourId");

        Object[] row = (Object[]) entityManager.createNativeQuery(
                "select publication_status, ST_GeometryType(route_geometry), "
                        + "ST_SRID(route_geometry), original_filename, source_reference "
                        + "from tour join gpx_track on gpx_track.tour_id = tour.id "
                        + "where tour.id = :id"
        ).setParameter("id", tourId).getSingleResult();

        assertEquals("DRAFT", row[0]);
        assertEquals("ST_LineString", row[1]);
        assertEquals(4326, ((Number) row[2]).intValue());
        assertEquals("original.gpx", row[3]);
        assertNotEquals("original.gpx", row[4]);
        assertTrue(Files.exists(STORAGE_ROOT.resolve((String) row[4])));
        assertEquals("<gpx>untouched</gpx>", Files.readString(STORAGE_ROOT.resolve((String) row[4])));
    }

    @Test
    void missingPartsAndEmptyFileReturnBadRequest() {
        given()
                .multiPart("gpxFile", "original.gpx", "content".getBytes(), "application/gpx+xml")
                .when().post("/api/management/imports/gpx")
                .then().statusCode(400);

        given()
                .multiPart("metadata", "metadata.json", validMetadata(), "application/json")
                .when().post("/api/management/imports/gpx")
                .then().statusCode(400);

        given()
                .multiPart("metadata", "metadata.json", validMetadata(), "application/json")
                .multiPart("gpxFile", "original.gpx", new byte[0], "application/gpx+xml")
                .when().post("/api/management/imports/gpx")
                .then().statusCode(400);
    }

    @Test
    void malformedJsonReturnsBadRequest() {
        given()
                .multiPart("metadata", "metadata.json", "{not-json", "application/json")
                .multiPart("gpxFile", "original.gpx", "content".getBytes(), "application/gpx+xml")
                .when().post("/api/management/imports/gpx")
                .then().statusCode(400);
    }

    @Test
    void semanticallyInvalidNormalizedDataReturnsUnprocessableEntity() {
        String invalid = validMetadata().replace("\"distanceKm\": 35.60", "\"distanceKm\": -1.00");

        given()
                .multiPart("metadata", "metadata.json", invalid, "application/json")
                .multiPart("gpxFile", "original.gpx", "content".getBytes(), "application/gpx+xml")
                .when().post("/api/management/imports/gpx")
                .then().statusCode(422);
    }

    @Test
    void wrongGeoJsonGeometryTypeReturnsUnprocessableEntity() {
        String invalid = validMetadata().replace("\"LineString\"", "\"Point\"");

        given()
                .multiPart("metadata", "metadata.json", invalid, "application/json")
                .multiPart("gpxFile", "original.gpx", "content".getBytes(), "application/gpx+xml")
                .when().post("/api/management/imports/gpx")
                .then().statusCode(422);
    }

    @Test
    void garminImportPersistsSourceIdentityAndDuplicateReturnsExistingTour() {
        Response created = postImport(garminMetadata("123456789"), "garmin.gpx");
        assertEquals(201, created.statusCode());
        long tourId = created.jsonPath().getLong("tourId");

        given()
                .when().get("/api/management/imports/gpx/sources/GARMIN_CONNECT/123456789")
                .then()
                .statusCode(200)
                .body("tourId", equalTo((int) tourId))
                .body("status", equalTo("DRAFT"));

        Response duplicate = postImport(garminMetadata("123456789"), "duplicate.gpx");
        assertEquals(200, duplicate.statusCode());
        assertEquals(tourId, duplicate.jsonPath().getLong("tourId"));

        Object[] counts = (Object[]) entityManager.createNativeQuery(
                "select (select count(*) from tour), (select count(*) from gpx_track)"
        ).getSingleResult();
        assertEquals(1L, ((Number) counts[0]).longValue());
        assertEquals(1L, ((Number) counts[1]).longValue());

        Object[] source = (Object[]) entityManager.createNativeQuery(
                "select source_provider, source_activity_id from gpx_track where tour_id = :id"
        ).setParameter("id", tourId).getSingleResult();
        assertEquals("GARMIN_CONNECT", source[0]);
        assertEquals("123456789", source[1]);
    }

    @Test
    void concurrentGarminImportsCreateExactlyOneTour() throws Exception {
        String metadata = garminMetadata("987654321");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Response> request = () -> {
            ready.countDown();
            start.await();
            return postImport(metadata, "concurrent.gpx");
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Response> first = executor.submit(request);
            Future<Response> second = executor.submit(request);
            ready.await();
            start.countDown();

            Response firstResponse = first.get();
            Response secondResponse = second.get();
            assertTrue(List.of(200, 201).contains(firstResponse.statusCode()));
            assertTrue(List.of(200, 201).contains(secondResponse.statusCode()));
            assertNotEquals(firstResponse.statusCode(), secondResponse.statusCode());
            assertEquals(firstResponse.jsonPath().getLong("tourId"),
                    secondResponse.jsonPath().getLong("tourId"));
        }

        Number tourCount = (Number) entityManager.createNativeQuery(
                "select count(*) from tour").getSingleResult();
        Number trackCount = (Number) entityManager.createNativeQuery(
                "select count(*) from gpx_track").getSingleResult();
        assertEquals(1L, tourCount.longValue());
        assertEquals(1L, trackCount.longValue());
        try (Stream<Path> paths = Files.list(STORAGE_ROOT)) {
            assertEquals(1L, paths.count());
        }
    }

    @Test
    void invalidSourceIdentityIsRejectedBeforeImport() {
        postImport(garminMetadata("00123"), "invalid.gpx")
                .then()
                .statusCode(422);
    }

    private static Response postImport(String metadata, String filename) {
        return given()
                .multiPart("metadata", "metadata.json", metadata, "application/json")
                .multiPart("gpxFile", filename, "<gpx>untouched</gpx>".getBytes(),
                        "application/gpx+xml")
                .when()
                .post("/api/management/imports/gpx");
    }

    private static String garminMetadata(String activityId) {
        return validMetadata().replace(
                "\n}",
                ",\n  \"sourceProvider\": \"GARMIN_CONNECT\",\n"
                        + "  \"sourceActivityId\": \"" + activityId + "\"\n}"
        );
    }

    private static String validMetadata() {
        return """
                {
                  "distanceKm": 35.60,
                  "totalAscentMeters": 1650,
                  "totalDescentMeters": 1600,
                  "lowestPointMeters": 923,
                  "highestPointMeters": 2677,
                  "tourDate": "2025-07-12",
                  "elevationProfile": [
                    {"distanceKm": 0.000, "elevationMeters": 923},
                    {"distanceKm": 35.600, "elevationMeters": 950}
                  ],
                  "routeGeometry": {
                    "type": "LineString",
                    "coordinates": [[13.5321, 46.4776], [13.5432, 46.4890]]
                  }
                }
                """;
    }
}
