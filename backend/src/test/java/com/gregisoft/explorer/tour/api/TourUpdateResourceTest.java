package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.entity.GpxTrack;
import com.gregisoft.explorer.tour.entity.Tour;
import com.gregisoft.explorer.tour.repository.TourRepository;
import com.gregisoft.explorer.tour.type.BikeType;
import com.gregisoft.explorer.tour.type.ExposureRating;
import com.gregisoft.explorer.tour.type.RouteType;
import com.gregisoft.explorer.tour.type.SurfaceType;
import com.gregisoft.explorer.tour.type.TechnicalDifficulty;
import com.gregisoft.explorer.tour.value.DurationRange;
import com.gregisoft.explorer.tour.value.ElevationProfileSample;
import com.gregisoft.explorer.tour.value.StartLocation;
import com.gregisoft.explorer.tour.value.SurfaceShare;
import com.gregisoft.explorer.tour.value.TourStatistics;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static com.gregisoft.explorer.test.TestDatabaseSafety.assertUsingIsolatedTestDatabase;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class TourUpdateResourceTest {

    private static final LocalDate IMPORTED_DATE = LocalDate.of(2025, 7, 12);

    @Inject
    EntityManager entityManager;

    @Inject
    TourRepository repository;

    private long importedDateTourId;
    private long noDateTourId;
    private long publishedTourId;

    @BeforeEach
    @Transactional
    void setUp() {
        assertUsingIsolatedTestDatabase(entityManager);
        entityManager.createNativeQuery("update tour set cover_photo_id = null").executeUpdate();
        entityManager.createNativeQuery("delete from tour").executeUpdate();

        importedDateTourId = persistImportedDraft(IMPORTED_DATE);
        noDateTourId = persistImportedDraft(null);

        Tour published = completeTour();
        published.publish(OffsetDateTime.of(2026, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC));
        repository.persistAndFlush(published);
        publishedTourId = published.getId();
    }

    @Test
    void completesImportedDraftAndPreservesGpxDerivedData() {
        given()
                .contentType(ContentType.JSON)
                .body(completePatch())
                .when().patch("/api/management/tours/{id}", importedDateTourId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("title", equalTo("Mangart via Italy"))
                .body("countryCode", equalTo("IT"))
                .body("startLocation.name", equalTo("Passo Predil"))
                .body("estimatedDuration.minimumHours", equalTo(5.0f))
                .body("technicalDifficulty", equalTo("S4"))
                .body("recommendedBikeTypes", hasSize(2))
                .body("surfaceComposition", hasSize(4))
                .body("youtubeVideoId", equalTo("AbCdEfGhI_1"))
                .body("publicationStatus", equalTo("DRAFT"))
                .body("publishedAt", nullValue())
                .body("tourDate", equalTo("2025-07-12"))
                .body("statistics.distanceKm", equalTo(35.60f))
                .body("statistics.totalAscentMeters", equalTo(1650))
                .body("elevationProfile", hasSize(2))
                .body("routeGeometry.type", equalTo("LineString"))
                .body("routeGeometry.coordinates", hasSize(2));

        Object[] gpxData = (Object[]) entityManager.createNativeQuery(
                "select original_filename, source_reference "
                        + "from gpx_track where tour_id = :tourId"
        ).setParameter("tourId", importedDateTourId).getSingleResult();
        assertEquals("mangart.gpx", gpxData[0]);
        assertEquals("private/mangart.gpx", gpxData[1]);
    }

    @Test
    void updatesSuppliedFieldsWithoutRequiringOtherIncompleteDraftFields() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Testna MTB tura",
                          "shortDescription": "Prva tura, posodobljena prek Explorer Management API."
                        }
                        """)
                .when().patch("/api/management/tours/{id}", importedDateTourId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Testna MTB tura"))
                .body(
                        "shortDescription",
                        equalTo("Prva tura, posodobljena prek Explorer Management API.")
                )
                .body("mountainRange", nullValue())
                .body("countryCode", nullValue())
                .body("technicalDescription", nullValue())
                .body("technicalDifficulty", nullValue());

        patch(importedDateTourId, "{\"mountainRange\":\"   \"}", 422);
    }

    @Test
    void omittedFieldsArePreservedAndCollectionsAreReplacedAtomically() {
        patch(importedDateTourId, completePatch(), 200);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Mangart Grand Traverse",
                          "recommendedBikeTypes": [],
                          "surfaceComposition": []
                        }
                        """)
                .when().patch("/api/management/tours/{id}", importedDateTourId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Mangart Grand Traverse"))
                .body("mountainRange", equalTo("Julian Alps"))
                .body("technicalDifficulty", equalTo("S4"))
                .body("recommendedBikeTypes", empty())
                .body("surfaceComposition", empty());
    }

    @Test
    void explicitNullIsRejectedExceptForYoutubeVideoId() {
        patch(importedDateTourId, completePatch(), 200);

        given()
                .contentType(ContentType.JSON)
                .body("{\"youtubeVideoId\":null}")
                .when().patch("/api/management/tours/{id}", importedDateTourId)
                .then()
                .statusCode(200)
                .body("youtubeVideoId", nullValue());

        patch(importedDateTourId, "{\"shortDescription\":null}", 422);
        patch(importedDateTourId, "{\"recommendedBikeTypes\":null}", 422);
        patch(importedDateTourId, "{\"surfaceComposition\":null}", 422);
    }

    @Test
    void tourDateCanOnlyFillAnAbsentImportedDate() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"tourDate\":\"2025-08-03\"}")
                .when().patch("/api/management/tours/{id}", noDateTourId)
                .then()
                .statusCode(200)
                .body("tourDate", equalTo("2025-08-03"));

        patch(noDateTourId, "{\"tourDate\":\"2025-08-04\"}", 422);
        patch(importedDateTourId, "{\"tourDate\":\"2025-08-04\"}", 422);
        patch(importedDateTourId, "{\"tourDate\":null}", 422);
    }

    @Test
    void invalidPatchRollsBackEveryField() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Must be rolled back",
                          "mountainRange": "Julian Alps",
                          "countryCode": "IT",
                          "technicalDifficulty": "S4",
                          "physicalDifficulty": 6,
                          "exposureRating": "E3",
                          "scenicRating": 5,
                          "carryingPercentage": 15
                        }
                        """)
                .when().patch("/api/management/tours/{id}", importedDateTourId)
                .then()
                .statusCode(422);

        given()
                .when().get("/api/management/tours/{id}", importedDateTourId)
                .then()
                .statusCode(200)
                .body("title", nullValue())
                .body("mountainRange", nullValue())
                .body("countryCode", nullValue());
    }

    @Test
    void rejectsUnknownReadOnlyMalformedAndIncompleteValues() {
        patch(importedDateTourId, "{\"statistics\":{}}", 400);
        patch(importedDateTourId, "{\"publicationStatus\":\"PUBLISHED\"}", 400);
        patch(importedDateTourId, "{\"unexpected\":true}", 400);
        patch(importedDateTourId, "{\"routeType\":\"ROUND_TRIP\"}", 400);
        patch(importedDateTourId, "{\"startLocation\":{\"name\":\"Passo Predil\"}}", 422);
        patch(importedDateTourId, "{\"estimatedDuration\":null}", 422);
        patch(importedDateTourId, "{\"recommendedBikeTypes\":[\"XC\",\"XC\"]}", 422);
        patch(importedDateTourId, "[]", 400);
    }

    @Test
    void rejectsSemanticallyInvalidValues() {
        patch(importedDateTourId, """
                {
                  "estimatedDuration": {"minimumHours": 5.2, "maximumHours": 6.5}
                }
                """, 422);
        patch(importedDateTourId, """
                {
                  "surfaceComposition": [
                    {"surfaceType":"GRAVEL","percentage":40},
                    {"surfaceType":"GRAVEL","percentage":60}
                  ]
                }
                """, 422);
        patch(importedDateTourId, "{\"youtubeVideoId\":\"too-short\"}", 422);
        patch(importedDateTourId, "{\"youtubeVideoId\":\"   \"}", 422);
        patch(
                importedDateTourId,
                "{\"title\":\"" + "x".repeat(201) + "\"}",
                422
        );
        patch(importedDateTourId, """
                {
                  "startLocation": {
                    "name": "Invalid latitude",
                    "latitude": 91,
                    "longitude": 13.5321
                  }
                }
                """, 422);
    }

    @Test
    void missingTourReturnsNotFoundAndPublishedTourReturnsConflict() {
        patch(Long.MAX_VALUE, "{}", 404);
        patch(publishedTourId, "{\"title\":\"Changed\"}", 409);
    }

    private long persistImportedDraft(LocalDate tourDate) {
        Tour tour = Tour.createDraft();
        tour.setStatistics(new TourStatistics(
                new BigDecimal("35.60"), 1650, 1600, 923, 2677
        ));
        if (tourDate != null) {
            tour.setTourDate(tourDate);
        }
        tour.setElevationProfile(List.of(
                new ElevationProfileSample(new BigDecimal("0.000"), 923),
                new ElevationProfileSample(new BigDecimal("35.600"), 950)
        ));
        tour.setRouteGeometry(lineString());
        tour.attachGpxTrack(new GpxTrack("mangart.gpx", "private/mangart.gpx"));
        repository.persistAndFlush(tour);
        return tour.getId();
    }

    private static Tour completeTour() {
        Tour tour = Tour.createDraft();
        tour.setIdentity("Published Tour", "Julian Alps", "SI");
        tour.setStartLocation(new StartLocation(
                "Passo Predil",
                new BigDecimal("46.4776000"),
                new BigDecimal("13.5321000")
        ));
        tour.setDescriptions("Published summary.", "Published technical description.");
        tour.setStatistics(new TourStatistics(
                new BigDecimal("35.60"), 1650, 1600, 923, 2677
        ));
        tour.setEstimatedDuration(new DurationRange(
                new BigDecimal("5.0"), new BigDecimal("6.5")
        ));
        tour.setTourDate(IMPORTED_DATE);
        tour.setRouteGeometry(lineString());
        tour.setDifficulty(TechnicalDifficulty.S4, 4, ExposureRating.E3, 5, 15);
        tour.setRouteType(RouteType.LOOP);
        tour.replaceRecommendedBikeTypes(Set.of(BikeType.ALL_MOUNTAIN));
        tour.replaceSurfaceComposition(List.of(
                new SurfaceShare(SurfaceType.GRAVEL, 40),
                new SurfaceShare(SurfaceType.SINGLETRACK, 60)
        ));
        tour.setElevationProfile(List.of(
                new ElevationProfileSample(new BigDecimal("0.000"), 923),
                new ElevationProfileSample(new BigDecimal("35.600"), 950)
        ));
        tour.attachGpxTrack(new GpxTrack("published.gpx", "private/published.gpx"));
        return tour;
    }

    private static LineString lineString() {
        return new GeometryFactory(new PrecisionModel(), 4326).createLineString(new Coordinate[]{
                new Coordinate(13.5321, 46.4776),
                new Coordinate(13.5432, 46.4890)
        });
    }

    private static String completePatch() {
        return """
                {
                  "title": "Mangart via Italy",
                  "mountainRange": "Julian Alps",
                  "countryCode": "it",
                  "startLocation": {
                    "name": "Passo Predil",
                    "latitude": 46.4776000,
                    "longitude": 13.5321000
                  },
                  "shortDescription": "Demanding alpine MTB tour.",
                  "technicalDescription": "A long ascent followed by a technical descent.",
                  "estimatedDuration": {
                    "minimumHours": 5.0,
                    "maximumHours": 6.5
                  },
                  "technicalDifficulty": "S4",
                  "physicalDifficulty": 4,
                  "exposureRating": "E3",
                  "scenicRating": 5,
                  "carryingPercentage": 15,
                  "routeType": "LOOP",
                  "recommendedBikeTypes": ["ALL_MOUNTAIN", "ENDURO"],
                  "surfaceComposition": [
                    {"surfaceType": "ASPHALT", "percentage": 15},
                    {"surfaceType": "GRAVEL", "percentage": 30},
                    {"surfaceType": "FOREST_ROAD", "percentage": 25},
                    {"surfaceType": "SINGLETRACK", "percentage": 30}
                  ],
                  "youtubeVideoId": "AbCdEfGhI_1"
                }
                """;
    }

    private static void patch(long tourId, String body, int expectedStatus) {
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().patch("/api/management/tours/{id}", tourId)
                .then().statusCode(expectedStatus);
    }
}
