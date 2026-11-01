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
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.gregisoft.explorer.test.TestDatabaseSafety.assertUsingIsolatedTestDatabase;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TourDiscoveryResourceTest {

    private static final String BBOX = "13,45,15,47";
    private static final OffsetDateTime BASE_PUBLICATION_TIME =
            OffsetDateTime.of(2026, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);
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
    void usesInclusiveDiscoveryPointBoundsAndExcludesOutsideAndDraftTours() {
        long inside = createPublishedTour(
                "Inside", "SI", TechnicalDifficulty.S2,
                "20.00", 1_000, 14.0, 46.0, BASE_PUBLICATION_TIME
        );
        long boundary = createPublishedTour(
                "Boundary", "SI", TechnicalDifficulty.S3,
                "30.00", 1_500, 13.0, 46.0, BASE_PUBLICATION_TIME.minusHours(1)
        );
        long outside = createPublishedTour(
                "Outside", "SI", TechnicalDifficulty.S2,
                "40.00", 2_000, 16.0, 46.0, BASE_PUBLICATION_TIME.minusHours(2)
        );
        long draft = createDraftTour("Draft", 14.0, 46.0);

        Response response = request(BBOX).get("/api/tours");

        response.then().statusCode(200).body("items", hasSize(2));
        List<Long> ids = ids(response);
        assertEquals(List.of(inside, boundary), ids);
        assertFalse(ids.contains(outside));
        assertFalse(ids.contains(draft));
        assertEquals(ids.size(), new HashSet<>(ids).size());
    }

    @Test
    void returnsLightweightPublishedSummaryWithoutInternalOrLargeFields() {
        long id = createPublishedTour(
                "Summary", "SI", TechnicalDifficulty.S4,
                "35.60", 1_650, 14.0, 46.0, BASE_PUBLICATION_TIME
        );

        Response response = request(BBOX).get("/api/tours");

        response.then()
                .statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].id", equalTo((int) id))
                .body("items[0].title", equalTo("Summary"))
                .body("items[0].countryCode", equalTo("SI"))
                .body("items[0].distanceKm", equalTo(35.60f))
                .body("items[0].totalAscentMeters", equalTo(1_650))
                .body("items[0].technicalDifficulty", equalTo("S4"))
                .body("items[0].publishedAt", notNullValue())
                .body("hasMore", equalTo(false))
                .body("nextCursor", nullValue());

        String body = response.asString();
        assertFalse(body.contains("routeGeometry"));
        assertFalse(body.contains("elevationProfile"));
        assertFalse(body.contains("photos"));
        assertFalse(body.contains("gpxTrack"));
        assertFalse(body.contains("private/Summary.gpx"));
        assertFalse(body.contains("sourceReference"));
    }

    @Test
    void appliesCountryDifficultyDistanceAndAscentFiltersTogether() {
        long first = createPublishedTour(
                "First", "SI", TechnicalDifficulty.S2,
                "20.00", 1_000, 14.0, 46.0, BASE_PUBLICATION_TIME
        );
        long second = createPublishedTour(
                "Second", "SI", TechnicalDifficulty.S3,
                "40.00", 2_000, 14.1, 46.0, BASE_PUBLICATION_TIME.minusHours(1)
        );
        createPublishedTour(
                "Wrong country", "IT", TechnicalDifficulty.S2,
                "25.00", 1_200, 14.2, 46.0, BASE_PUBLICATION_TIME.minusHours(2)
        );
        createPublishedTour(
                "Wrong difficulty", "SI", TechnicalDifficulty.S4,
                "25.00", 1_200, 14.3, 46.0, BASE_PUBLICATION_TIME.minusHours(3)
        );
        createPublishedTour(
                "Too long", "SI", TechnicalDifficulty.S2,
                "60.00", 1_200, 14.4, 46.0, BASE_PUBLICATION_TIME.minusHours(4)
        );
        createPublishedTour(
                "Too much ascent", "SI", TechnicalDifficulty.S2,
                "30.00", 3_000, 14.5, 46.0, BASE_PUBLICATION_TIME.minusHours(5)
        );

        Response response = request(BBOX)
                .queryParam("countryCode", "si")
                .queryParam("technicalDifficulty", "S2,S3")
                .queryParam("distanceMinKm", "15")
                .queryParam("distanceMaxKm", "45")
                .queryParam("ascentMinMeters", "900")
                .queryParam("ascentMaxMeters", "2100")
                .get("/api/tours");

        response.then().statusCode(200).body("items", hasSize(2));
        assertEquals(List.of(first, second), ids(response));
    }

    @Test
    void rejectsInvalidBoundingBoxesRangesFiltersAndPageSizes() {
        assertBadRequest(given(), "The bbox query parameter is required.");
        assertBadRequest(request("13,45,15"), "Invalid bounding box");
        assertBadRequest(request("NaN,45,15,47"), "finite number");
        assertBadRequest(request("-181,45,15,47"), "longitude");
        assertBadRequest(request("13,-91,15,47"), "latitude");
        assertBadRequest(request("13,47,15,45"), "south must be less than north");
        assertBadRequest(request("170,45,-170,47"), "Antimeridian-crossing");
        assertBadRequest(request(BBOX).queryParam("distanceMinKm", "30")
                .queryParam("distanceMaxKm", "20"), "distanceMinKm");
        assertBadRequest(request(BBOX).queryParam("ascentMinMeters", "2000")
                .queryParam("ascentMaxMeters", "1000"), "ascentMinMeters");
        assertBadRequest(request(BBOX).queryParam("technicalDifficulty", "S8"),
                "technicalDifficulty");
        assertBadRequest(request(BBOX).queryParam("countryCode", "SVN"), "countryCode");
        assertBadRequest(request(BBOX).queryParam("pageSize", "0"), "pageSize");
        assertBadRequest(request(BBOX).queryParam("pageSize", "51"), "pageSize");

        request(BBOX).queryParam("pageSize", "50")
                .get("/api/tours")
                .then().statusCode(200);
    }

    @Test
    void defaultsToTwentyFourItemsAndReturnsNextCursor() {
        for (int index = 0; index < 25; index++) {
            createPublishedTour(
                    "Tour " + index,
                    "SI",
                    TechnicalDifficulty.S2,
                    "20.00",
                    1_000,
                    14.0,
                    46.0,
                    BASE_PUBLICATION_TIME.minusMinutes(index)
            );
        }

        request(BBOX).get("/api/tours")
                .then()
                .statusCode(200)
                .body("items", hasSize(24))
                .body("hasMore", equalTo(true))
                .body("nextCursor", notNullValue());
    }

    @Test
    void paginatesDeterministicallyWithIdenticalPublicationTimestamps() {
        List<Long> created = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            created.add(createPublishedTour(
                    "Same time " + index,
                    "SI",
                    TechnicalDifficulty.S2,
                    "20.00",
                    1_000,
                    14.0,
                    46.0,
                    BASE_PUBLICATION_TIME
            ));
        }
        List<Long> expected = created.reversed();

        Response first = request(BBOX).queryParam("pageSize", "2").get("/api/tours");
        String firstCursor = first.jsonPath().getString("nextCursor");
        first.then().statusCode(200).body("hasMore", equalTo(true));

        Response second = request(BBOX)
                .queryParam("pageSize", "2")
                .queryParam("cursor", firstCursor)
                .get("/api/tours");
        String secondCursor = second.jsonPath().getString("nextCursor");
        second.then().statusCode(200).body("hasMore", equalTo(true));

        Response third = request(BBOX)
                .queryParam("pageSize", "2")
                .queryParam("cursor", secondCursor)
                .get("/api/tours");
        third.then()
                .statusCode(200)
                .body("items", hasSize(1))
                .body("hasMore", equalTo(false))
                .body("nextCursor", nullValue());

        List<Long> actual = new ArrayList<>();
        actual.addAll(ids(first));
        actual.addAll(ids(second));
        actual.addAll(ids(third));
        assertEquals(expected, actual);
        assertEquals(actual.size(), new HashSet<>(actual).size());
    }

    @Test
    void rejectsMalformedUnsupportedAndMismatchedCursors() {
        createPublishedTour(
                "Cursor one", "SI", TechnicalDifficulty.S2,
                "20.00", 1_000, 14.0, 46.0, BASE_PUBLICATION_TIME
        );
        createPublishedTour(
                "Cursor two", "SI", TechnicalDifficulty.S2,
                "20.00", 1_000, 14.0, 46.0, BASE_PUBLICATION_TIME.minusHours(1)
        );
        Response first = request(BBOX).queryParam("pageSize", "1").get("/api/tours");
        String cursor = first.jsonPath().getString("nextCursor");

        assertBadRequest(request(BBOX).queryParam("cursor", "not-base64"), "Invalid");
        assertBadRequest(request(BBOX).queryParam("cursor", cursor)
                .queryParam("countryCode", "SI"), "does not match");
        assertBadRequest(request("13,45,14.9,47").queryParam("cursor", cursor),
                "does not match");

        String unsupported = Base64.getUrlEncoder().withoutPadding().encodeToString("""
                {"version":99,"snapshotAt":"2026-01-15T11:00:00Z",\
                "publishedAt":"2026-01-15T10:30:00Z","id":1,\
                "criteriaFingerprint":"unused"}
                """.getBytes(StandardCharsets.UTF_8));
        assertBadRequest(request(BBOX).queryParam("cursor", unsupported), "Invalid");
    }

    @Test
    void paginationSnapshotExcludesToursPublishedAfterTheFirstPage() {
        long first = createPublishedTour(
                "Original first", "SI", TechnicalDifficulty.S2,
                "20.00", 1_000, 14.0, 46.0, BASE_PUBLICATION_TIME
        );
        long second = createPublishedTour(
                "Original second", "SI", TechnicalDifficulty.S2,
                "20.00", 1_000, 14.0, 46.0, BASE_PUBLICATION_TIME.minusHours(1)
        );

        Response firstPage = request(BBOX).queryParam("pageSize", "1").get("/api/tours");
        assertEquals(List.of(first), ids(firstPage));
        String cursor = firstPage.jsonPath().getString("nextCursor");

        long newlyPublished = createPublishedTour(
                "New publication", "SI", TechnicalDifficulty.S2,
                "20.00", 1_000, 14.0, 46.0,
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5)
        );

        Response secondPage = request(BBOX)
                .queryParam("pageSize", "1")
                .queryParam("cursor", cursor)
                .get("/api/tours");
        assertEquals(List.of(second), ids(secondPage));
        assertFalse(ids(secondPage).contains(newlyPublished));
    }

    private long createPublishedTour(
            String title,
            String countryCode,
            TechnicalDifficulty technicalDifficulty,
            String distanceKm,
            int ascentMeters,
            double midpointLongitude,
            double midpointLatitude,
            OffsetDateTime publicationTime
    ) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Tour tour = completeTour(
                    title,
                    countryCode,
                    technicalDifficulty,
                    distanceKm,
                    ascentMeters,
                    midpointLongitude,
                    midpointLatitude
            );
            tour.publish(publicationTime);
            repository.persistAndFlush(tour);
            return tour.getId();
        });
    }

    private long createDraftTour(String title, double longitude, double latitude) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Tour tour = completeTour(
                    title,
                    "SI",
                    TechnicalDifficulty.S2,
                    "20.00",
                    1_000,
                    longitude,
                    latitude
            );
            repository.persistAndFlush(tour);
            return tour.getId();
        });
    }

    private static Tour completeTour(
            String title,
            String countryCode,
            TechnicalDifficulty technicalDifficulty,
            String distanceKm,
            int ascentMeters,
            double midpointLongitude,
            double midpointLatitude
    ) {
        Tour tour = Tour.createDraft();
        tour.setIdentity(title, "Julian Alps", countryCode);
        tour.setStartLocation(new StartLocation(
                "Start",
                decimal(midpointLatitude),
                decimal(midpointLongitude)
        ));
        tour.setDescriptions("Short description.", "Technical description.");
        tour.setStatistics(new TourStatistics(
                new BigDecimal(distanceKm),
                ascentMeters,
                ascentMeters,
                500,
                1_500
        ));
        tour.setEstimatedDuration(new DurationRange(
                new BigDecimal("3.0"),
                new BigDecimal("4.0")
        ));
        tour.setTourDate(LocalDate.of(2025, 8, 3));
        tour.setRouteGeometry(lineString(midpointLongitude, midpointLatitude));
        tour.setDifficulty(technicalDifficulty, 3, ExposureRating.E2, 4, 10);
        tour.setRouteType(RouteType.LOOP);
        tour.replaceRecommendedBikeTypes(Set.of(BikeType.XC));
        tour.replaceSurfaceComposition(List.of(new SurfaceShare(SurfaceType.GRAVEL, 100)));
        tour.setElevationProfile(List.of(
                new ElevationProfileSample(new BigDecimal("0.000"), 500),
                new ElevationProfileSample(new BigDecimal(distanceKm).setScale(3), 1_500)
        ));
        tour.attachGpxTrack(new GpxTrack(title + ".gpx", "private/" + title + ".gpx"));
        return tour;
    }

    private static LineString lineString(double midpointLongitude, double midpointLatitude) {
        return GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(midpointLongitude - 0.01, midpointLatitude),
                new Coordinate(midpointLongitude + 0.01, midpointLatitude)
        });
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(7);
    }

    private static RequestSpecification request(String bbox) {
        return given().queryParam("bbox", bbox);
    }

    private static List<Long> ids(Response response) {
        return response.jsonPath().getList("items.id", Number.class).stream()
                .map(Number::longValue)
                .toList();
    }

    private static void assertBadRequest(RequestSpecification request, String messageFragment) {
        Response response = request.get("/api/tours");
        response.then().statusCode(400);
        assertTrue(response.jsonPath().getString("message").contains(messageFragment));
    }
}
