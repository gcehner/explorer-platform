package com.gregisoft.explorer.tour.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gregisoft.explorer.tour.entity.GpxTrack;
import com.gregisoft.explorer.tour.entity.Photo;
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
import io.restassured.response.Response;
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

import static io.restassured.RestAssured.given;
import static com.gregisoft.explorer.test.TestDatabaseSafety.assertUsingIsolatedTestDatabase;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TourReadResourceTest {

    private static final OffsetDateTime PUBLICATION_TIME =
            OffsetDateTime.of(2026, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

    @Inject
    EntityManager entityManager;

    @Inject
    TourRepository repository;

    @Inject
    ObjectMapper objectMapper;

    private long draftTourId;
    private long publishedTourId;
    private long coverPhotoId;

    @BeforeEach
    @Transactional
    void setUp() {
        assertUsingIsolatedTestDatabase(entityManager);
        entityManager.createNativeQuery("update tour set cover_photo_id = null").executeUpdate();
        entityManager.createNativeQuery("delete from tour").executeUpdate();

        Tour draft = Tour.createDraft();
        draft.setStatistics(new TourStatistics(
                new BigDecimal("8.25"),
                420,
                415,
                520,
                910
        ));
        draft.setTourDate(LocalDate.of(2025, 7, 12));
        draft.setElevationProfile(List.of(
                new ElevationProfileSample(new BigDecimal("8.250"), 525),
                new ElevationProfileSample(new BigDecimal("0.000"), 520),
                new ElevationProfileSample(new BigDecimal("4.125"), 910)
        ).stream().sorted((left, right) ->
                left.getDistanceKm().compareTo(right.getDistanceKm())).toList());
        draft.setRouteGeometry(lineString());
        draft.attachGpxTrack(new GpxTrack("draft-source.gpx", "private/draft-source.gpx"));
        repository.persistAndFlush(draft);
        draftTourId = draft.getId();

        Tour published = completeTour();
        Photo secondPhoto = new Photo("photos/second.jpg", null, 1);
        Photo coverPhoto = new Photo("photos/cover.jpg", "Summit ridge", 0);
        published.addPhoto(secondPhoto);
        published.addPhoto(coverPhoto);
        repository.persistAndFlush(published);

        published.selectCoverPhoto(coverPhoto);
        published.publish(PUBLICATION_TIME);
        entityManager.flush();

        publishedTourId = published.getId();
        coverPhotoId = coverPhoto.getId();
    }

    @Test
    void managementReadsImportedDraftAndPreservesNullableFieldsAndJsonTypes() throws Exception {
        Response response = given()
                .when().get("/api/management/tours/{id}", draftTourId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) draftTourId))
                .body("publicationStatus", equalTo("DRAFT"))
                .body("title", nullValue())
                .body("mountainRange", nullValue())
                .body("countryCode", nullValue())
                .body("startLocation", nullValue())
                .body("shortDescription", nullValue())
                .body("technicalDescription", nullValue())
                .body("estimatedDuration", nullValue())
                .body("technicalDifficulty", nullValue())
                .body("physicalDifficulty", nullValue())
                .body("exposureRating", nullValue())
                .body("scenicRating", nullValue())
                .body("carryingPercentage", nullValue())
                .body("routeType", nullValue())
                .body("recommendedBikeTypes", empty())
                .body("surfaceComposition", empty())
                .body("publishedAt", nullValue())
                .body("photos", empty())
                .body("coverPhotoId", nullValue())
                .body("youtubeVideoId", nullValue())
                .extract().response();

        JsonNode json = objectMapper.readTree(response.asString());
        assertDecimalEquals("8.25", json.path("statistics").path("distanceKm"));
        assertEquals("2025-07-12", json.path("tourDate").textValue());
        assertDecimalEquals(
                "0.000",
                json.path("elevationProfile").get(0).path("distanceKm")
        );
        assertDecimalEquals(
                "4.125",
                json.path("elevationProfile").get(1).path("distanceKm")
        );
        assertDecimalEquals(
                "8.250",
                json.path("elevationProfile").get(2).path("distanceKm")
        );
        assertFullGeometry(json.path("routeGeometry"));

        String body = response.asString();
        assertFalse(body.contains("gpxTrack"));
        assertFalse(body.contains("originalFilename"));
        assertFalse(body.contains("sourceReference"));
        assertFalse(body.contains("draft-source.gpx"));
        assertFalse(body.contains("private/draft-source.gpx"));
    }

    @Test
    void publicDraftReturnsNotFound() {
        given()
                .when().get("/api/tours/{id}", draftTourId)
                .then().statusCode(404);
    }

    @Test
    void publicPublishedTourReturnsCompleteFrontendRepresentation() throws Exception {
        Response response = given()
                .when().get("/api/tours/{id}", publishedTourId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("title", equalTo("Mangart Traverse"))
                .body("mountainRange", equalTo("Julian Alps"))
                .body("countryCode", equalTo("SI"))
                .body("technicalDifficulty", equalTo("S4"))
                .body("physicalDifficulty", equalTo(5))
                .body("exposureRating", equalTo("E3"))
                .body("scenicRating", equalTo(5))
                .body("carryingPercentage", equalTo(15))
                .body("routeType", equalTo("POINT_TO_POINT"))
                .body("recommendedBikeTypes", contains("XC", "ENDURO"))
                .body("surfaceComposition", hasSize(2))
                .body("surfaceComposition[0].surfaceType", equalTo("SINGLETRACK"))
                .body("surfaceComposition[1].surfaceType", equalTo("GRAVEL"))
                .body("photos", hasSize(2))
                .body("photos[0].displayOrder", equalTo(0))
                .body("photos[0].caption", equalTo("Summit ridge"))
                .body("photos[1].displayOrder", equalTo(1))
                .body("coverPhotoId", equalTo((int) coverPhotoId))
                .body("youtubeVideoId", equalTo("AbCdEfGhI_1"))
                .body("publicationStatus", equalTo("PUBLISHED"))
                .extract().response();

        JsonNode json = objectMapper.readTree(response.asString());
        assertDecimalEquals("35.60", json.path("statistics").path("distanceKm"));
        assertDecimalEquals("5.0", json.path("estimatedDuration").path("minimumHours"));
        assertDecimalEquals("6.5", json.path("estimatedDuration").path("maximumHours"));
        assertEquals("2025-08-03", json.path("tourDate").textValue());
        assertEquals("2026-01-15T10:30:00Z", json.path("publishedAt").textValue());
        assertDecimalEquals(
                "0.000",
                json.path("elevationProfile").get(0).path("distanceKm")
        );
        assertDecimalEquals(
                "17.800",
                json.path("elevationProfile").get(1).path("distanceKm")
        );
        assertDecimalEquals(
                "35.600",
                json.path("elevationProfile").get(2).path("distanceKm")
        );
        assertFullGeometry(json.path("routeGeometry"));
        assertFalse(response.asString().contains("published-source.gpx"));
        assertFalse(response.asString().contains("private/published-source.gpx"));
    }

    @Test
    void managementReadsPublishedTour() {
        given()
                .when().get("/api/management/tours/{id}", publishedTourId)
                .then()
                .statusCode(200)
                .body("publicationStatus", equalTo("PUBLISHED"));
    }

    @Test
    void nonexistentTourReturnsNotFoundFromBothEndpoints() {
        long missingId = Long.MAX_VALUE;

        given()
                .when().get("/api/tours/{id}", missingId)
                .then().statusCode(404);

        given()
                .when().get("/api/management/tours/{id}", missingId)
                .then().statusCode(404);
    }

    private static Tour completeTour() {
        Tour tour = Tour.createDraft();
        tour.setIdentity("Mangart Traverse", "Julian Alps", "si");
        tour.setStartLocation(new StartLocation(
                "Passo Predil",
                new BigDecimal("46.4776000"),
                new BigDecimal("13.5321000")
        ));
        tour.setDescriptions(
                "Demanding alpine MTB tour.",
                "Technical singletrack with exposed sections."
        );
        tour.setStatistics(new TourStatistics(
                new BigDecimal("35.60"),
                1650,
                1600,
                923,
                2677
        ));
        tour.setEstimatedDuration(new DurationRange(
                new BigDecimal("5.0"),
                new BigDecimal("6.5")
        ));
        tour.setTourDate(LocalDate.of(2025, 8, 3));
        tour.setRouteGeometry(lineString());
        tour.setDifficulty(TechnicalDifficulty.S4, 5, ExposureRating.E3, 5, 15);
        tour.setRouteType(RouteType.POINT_TO_POINT);
        tour.replaceRecommendedBikeTypes(Set.of(BikeType.ENDURO, BikeType.XC));
        tour.replaceSurfaceComposition(List.of(
                new SurfaceShare(SurfaceType.SINGLETRACK, 70),
                new SurfaceShare(SurfaceType.GRAVEL, 30)
        ));
        tour.setElevationProfile(List.of(
                new ElevationProfileSample(new BigDecimal("0.000"), 923),
                new ElevationProfileSample(new BigDecimal("17.800"), 2677),
                new ElevationProfileSample(new BigDecimal("35.600"), 950)
        ));
        tour.attachGpxTrack(new GpxTrack(
                "published-source.gpx",
                "private/published-source.gpx"
        ));
        tour.setYoutubeVideoId("AbCdEfGhI_1");
        return tour;
    }

    private static LineString lineString() {
        return new GeometryFactory(new PrecisionModel(), 4326).createLineString(new Coordinate[]{
                new Coordinate(13.5321234, 46.4776543),
                new Coordinate(13.5432345, 46.4890654),
                new Coordinate(13.5543456, 46.5001765)
        });
    }

    private static void assertFullGeometry(JsonNode geometry) {
        assertEquals("LineString", geometry.path("type").textValue());
        JsonNode coordinates = geometry.path("coordinates");
        assertEquals(3, coordinates.size());
        assertDecimalEquals("13.5321234", coordinates.get(0).get(0));
        assertDecimalEquals("46.4776543", coordinates.get(0).get(1));
        assertDecimalEquals("13.5432345", coordinates.get(1).get(0));
        assertDecimalEquals("46.4890654", coordinates.get(1).get(1));
        assertDecimalEquals("13.5543456", coordinates.get(2).get(0));
        assertDecimalEquals("46.5001765", coordinates.get(2).get(1));
    }

    private static void assertDecimalEquals(String expected, JsonNode actual) {
        assertTrue(actual.isNumber());
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()));
    }
}
