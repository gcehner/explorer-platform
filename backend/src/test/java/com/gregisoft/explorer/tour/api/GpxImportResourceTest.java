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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class GpxImportResourceTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-gpx-storage");

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    void cleanState() throws IOException {
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
