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

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TourLatestResourceTest {

    private static final OffsetDateTime BASE_PUBLICATION_TIME =
            OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Inject
    EntityManager entityManager;

    @Inject
    TourRepository repository;

    @BeforeEach
    @Transactional
    void setUp() {
        assertUsingIsolatedTestDatabase(entityManager);
        entityManager.createNativeQuery("update tour set cover_photo_id = null").executeUpdate();
        entityManager.createNativeQuery("delete from tour").executeUpdate();
    }

    @Test
    void returnsSixNewestPublishedToursGloballyWithDeterministicTieBreaking() {
        long excludedOldest = createPublishedTour(
                "Oldest", 14.0, 46.0, BASE_PUBLICATION_TIME.minusDays(5)
        );
        long first = createPublishedTour(
                "First", 14.0, 46.0, BASE_PUBLICATION_TIME.minusDays(4)
        );
        long second = createPublishedTour(
                "Second", 14.0, 46.0, BASE_PUBLICATION_TIME.minusDays(3)
        );
        long third = createPublishedTour(
                "Third", 14.0, 46.0, BASE_PUBLICATION_TIME.minusDays(2)
        );
        long global = createPublishedTour(
                "Global", 120.0, 10.0, BASE_PUBLICATION_TIME.minusDays(1)
        );
        long tiedFirst = createPublishedTour(
                "Tied first", 14.0, 46.0, BASE_PUBLICATION_TIME
        );
        long tiedSecond = createPublishedTour(
                "Tied second", 14.0, 46.0, BASE_PUBLICATION_TIME
        );
        long draft = createDraftTour("Draft", 14.0, 46.0);

        Response response = given()
                .queryParam("bbox", "13,45,15,47")
                .get("/api/tours/latest");

        response.then().statusCode(200).body("items", hasSize(6));
        List<Long> ids = ids(response);
        assertEquals(List.of(tiedSecond, tiedFirst, global, third, second, first), ids);
        assertFalse(ids.contains(excludedOldest));
        assertFalse(ids.contains(draft));
    }

    @Test
    void honorsAnExplicitValidLimit() {
        createPublishedTour("First", 14.0, 46.0, BASE_PUBLICATION_TIME.minusHours(2));
        long second = createPublishedTour(
                "Second", 14.0, 46.0, BASE_PUBLICATION_TIME.minusHours(1)
        );
        long third = createPublishedTour("Third", 14.0, 46.0, BASE_PUBLICATION_TIME);

        Response response = given().queryParam("limit", "2").get("/api/tours/latest");

        response.then().statusCode(200).body("items", hasSize(2));
        assertEquals(List.of(third, second), ids(response));
    }

    @Test
    void rejectsInvalidAndExcessiveLimits() {
        assertBadLimit("0", "between 1 and 12");
        assertBadLimit("13", "between 1 and 12");
        assertBadLimit("not-a-number", "must be an integer");
    }

    @Test
    void returnsAnEmptyListWhenNoPublishedToursExist() {
        createDraftTour("Draft", 14.0, 46.0);

        given()
                .get("/api/tours/latest")
                .then()
                .statusCode(200)
                .body("items", hasSize(0));
    }

    @Test
    void returnsTheExistingSafePublicSummaryShape() {
        long id = createPublishedTour("Summary", 14.0, 46.0, BASE_PUBLICATION_TIME);

        Response response = given().get("/api/tours/latest");

        response.then()
                .statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].id", equalTo((int) id))
                .body("items[0].title", equalTo("Summary"))
                .body("items[0].technicalDifficulty", equalTo("S2"));

        String body = response.asString();
        assertFalse(body.contains("routeGeometry"));
        assertFalse(body.contains("elevationProfile"));
        assertFalse(body.contains("photos"));
        assertFalse(body.contains("gpxTrack"));
        assertFalse(body.contains("private/Summary.gpx"));
        assertFalse(body.contains("sourceReference"));
    }

    private long createPublishedTour(
            String title,
            double longitude,
            double latitude,
            OffsetDateTime publicationTime
    ) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Tour tour = completeTour(title, longitude, latitude);
            tour.publish(publicationTime);
            repository.persistAndFlush(tour);
            return tour.getId();
        });
    }

    private long createDraftTour(String title, double longitude, double latitude) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Tour tour = completeTour(title, longitude, latitude);
            repository.persistAndFlush(tour);
            return tour.getId();
        });
    }

    private static Tour completeTour(String title, double longitude, double latitude) {
        Tour tour = Tour.createDraft();
        tour.setIdentity(title, "Julian Alps", "SI");
        tour.setStartLocation(new StartLocation(
                "Start",
                decimal(latitude),
                decimal(longitude)
        ));
        tour.setDescriptions("Short description.", "Technical description.");
        tour.setStatistics(new TourStatistics(
                new BigDecimal("30.00"),
                1_200,
                1_200,
                500,
                1_500
        ));
        tour.setEstimatedDuration(new DurationRange(
                new BigDecimal("3.0"),
                new BigDecimal("4.0")
        ));
        tour.setTourDate(LocalDate.of(2025, 8, 3));
        tour.setRouteGeometry(lineString(longitude, latitude));
        tour.setDifficulty(TechnicalDifficulty.S2, 3, ExposureRating.E2, 4, 10);
        tour.setRouteType(RouteType.LOOP);
        tour.replaceRecommendedBikeTypes(Set.of(BikeType.XC));
        tour.replaceSurfaceComposition(List.of(new SurfaceShare(SurfaceType.GRAVEL, 100)));
        tour.setElevationProfile(List.of(
                new ElevationProfileSample(new BigDecimal("0.000"), 500),
                new ElevationProfileSample(new BigDecimal("30.000"), 1_500)
        ));
        tour.attachGpxTrack(new GpxTrack(title + ".gpx", "private/" + title + ".gpx"));
        return tour;
    }

    private static LineString lineString(double longitude, double latitude) {
        return GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(longitude - 0.01, latitude),
                new Coordinate(longitude + 0.01, latitude)
        });
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(7);
    }

    private static List<Long> ids(Response response) {
        return response.jsonPath().getList("items.id", Number.class).stream()
                .map(Number::longValue)
                .toList();
    }

    private static void assertBadLimit(String limit, String messageFragment) {
        Response response = given().queryParam("limit", limit).get("/api/tours/latest");
        response.then().statusCode(400);
        assertTrue(response.jsonPath().getString("message").contains(messageFragment));
    }
}
