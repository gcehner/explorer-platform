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

import static com.gregisoft.explorer.test.TestDatabaseSafety.assertUsingIsolatedTestDatabase;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class TourPublishResourceTest {

    @Inject
    EntityManager entityManager;

    @Inject
    TourRepository repository;

    private long completeDraftId;
    private long incompleteDraftId;
    private long missingBikeTypeDraftId;
    private long invalidSurfaceDraftId;

    @BeforeEach
    @Transactional
    void setUp() {
        assertUsingIsolatedTestDatabase(entityManager);
        entityManager.createNativeQuery("update tour set cover_photo_id = null").executeUpdate();
        entityManager.createNativeQuery("delete from tour").executeUpdate();

        completeDraftId = persist(completeDraft(true, 100));
        incompleteDraftId = persist(importedDraft());
        missingBikeTypeDraftId = persist(completeDraft(false, 100));
        invalidSurfaceDraftId = persist(completeDraft(true, 90));
    }

    @Test
    void publishesCompleteDraftAndMakesItPubliclyVisible() {
        given()
                .when().get("/api/tours/{id}", completeDraftId)
                .then().statusCode(404);

        Response response = given()
                .when().post("/api/management/tours/{id}/publish", completeDraftId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("publicationStatus", equalTo("PUBLISHED"))
                .body("publishedAt", notNullValue())
                .body("photos", empty())
                .body("coverPhotoId", nullValue())
                .body("youtubeVideoId", nullValue())
                .body("statistics.distanceKm", equalTo(35.60f))
                .extract().response();

        String publishedAt = response.jsonPath().getString("publishedAt");
        assertEquals(ZoneOffset.UTC, OffsetDateTime.parse(publishedAt).getOffset());

        given()
                .when().get("/api/tours/{id}", completeDraftId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) completeDraftId))
                .body("publicationStatus", equalTo("PUBLISHED"))
                .body("publishedAt", equalTo(publishedAt));

        Object[] persisted = (Object[]) entityManager.createNativeQuery(
                "select publication_status, published_at from tour where id = :id"
        ).setParameter("id", completeDraftId).getSingleResult();
        assertEquals("PUBLISHED", persisted[0]);
        assertNotNull(persisted[1]);

        Object[] gpxData = (Object[]) entityManager.createNativeQuery(
                "select original_filename, source_reference "
                        + "from gpx_track where tour_id = :id"
        ).setParameter("id", completeDraftId).getSingleResult();
        assertEquals("publish-test.gpx", gpxData[0]);
        assertEquals("private/publish-test.gpx", gpxData[1]);
    }

    @Test
    void repeatPublicationIsIdempotentAndPreservesOriginalTimestamp() {
        String firstPublishedAt = given()
                .when().post("/api/management/tours/{id}/publish", completeDraftId)
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("publishedAt");

        given()
                .when().post("/api/management/tours/{id}/publish", completeDraftId)
                .then()
                .statusCode(200)
                .body("publicationStatus", equalTo("PUBLISHED"))
                .body("publishedAt", equalTo(firstPublishedAt));
    }

    @Test
    void incompleteDraftReturnsValidationMessageAndRemainsPrivate() {
        given()
                .when().post("/api/management/tours/{id}/publish", incompleteDraftId)
                .then()
                .statusCode(422)
                .contentType(ContentType.JSON)
                .body("message", equalTo("Title is required before publication."));

        given()
                .when().get("/api/tours/{id}", incompleteDraftId)
                .then().statusCode(404);

        Object[] persisted = (Object[]) entityManager.createNativeQuery(
                "select publication_status, published_at from tour where id = :id"
        ).setParameter("id", incompleteDraftId).getSingleResult();
        assertEquals("DRAFT", persisted[0]);
        assertNull(persisted[1]);
    }

    @Test
    void missingRecommendedBikeTypeReturnsValidationError() {
        given()
                .when().post("/api/management/tours/{id}/publish", missingBikeTypeDraftId)
                .then()
                .statusCode(422)
                .body(
                        "message",
                        equalTo("A Tour must recommend at least one bike type before publication.")
                );
    }

    @Test
    void invalidSurfaceTotalReturnsValidationError() {
        given()
                .when().post("/api/management/tours/{id}/publish", invalidSurfaceDraftId)
                .then()
                .statusCode(422)
                .body(
                        "message",
                        equalTo("Surface composition must total 100 before publication.")
                );
    }

    @Test
    void missingTourReturnsNotFound() {
        given()
                .when().post("/api/management/tours/{id}/publish", Long.MAX_VALUE)
                .then().statusCode(404);
    }

    private long persist(Tour tour) {
        repository.persistAndFlush(tour);
        return tour.getId();
    }

    private static Tour importedDraft() {
        Tour tour = Tour.createDraft();
        addImportedData(tour);
        return tour;
    }

    private static Tour completeDraft(boolean includeBikeType, int surfaceTotal) {
        Tour tour = importedDraft();
        tour.setIdentity("Mangart via Italy", "Julian Alps", "IT");
        tour.setStartLocation(new StartLocation(
                "Passo Predil",
                new BigDecimal("46.4776000"),
                new BigDecimal("13.5321000")
        ));
        tour.setDescriptions(
                "Demanding alpine MTB tour.",
                "A long ascent followed by a technical descent."
        );
        tour.setEstimatedDuration(new DurationRange(
                new BigDecimal("5.0"), new BigDecimal("6.5")
        ));
        tour.setDifficulty(TechnicalDifficulty.S4, 4, ExposureRating.E3, 5, 15);
        tour.setRouteType(RouteType.LOOP);
        if (includeBikeType) {
            tour.replaceRecommendedBikeTypes(Set.of(BikeType.ALL_MOUNTAIN));
        }
        tour.replaceSurfaceComposition(List.of(
                new SurfaceShare(SurfaceType.GRAVEL, 40),
                new SurfaceShare(SurfaceType.SINGLETRACK, surfaceTotal - 40)
        ));
        return tour;
    }

    private static void addImportedData(Tour tour) {
        tour.setStatistics(new TourStatistics(
                new BigDecimal("35.60"), 1650, 1600, 923, 2677
        ));
        tour.setTourDate(LocalDate.of(2025, 7, 12));
        tour.setElevationProfile(List.of(
                new ElevationProfileSample(new BigDecimal("0.000"), 923),
                new ElevationProfileSample(new BigDecimal("35.600"), 950)
        ));
        tour.setRouteGeometry(lineString());
        tour.attachGpxTrack(new GpxTrack(
                "publish-test.gpx",
                "private/publish-test.gpx"
        ));
    }

    private static LineString lineString() {
        return new GeometryFactory(new PrecisionModel(), 4326).createLineString(new Coordinate[]{
                new Coordinate(13.5321, 46.4776),
                new Coordinate(13.5432, 46.4890)
        });
    }
}
