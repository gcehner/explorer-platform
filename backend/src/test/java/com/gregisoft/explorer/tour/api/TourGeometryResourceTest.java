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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.gregisoft.explorer.test.TestDatabaseSafety.assertUsingIsolatedTestDatabase;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TourGeometryResourceTest {

    private static final String BBOX = "13,45,15,47";
    private static final OffsetDateTime PUBLICATION_TIME =
            OffsetDateTime.of(2026, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
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
    void validatesRequiredParametersAndGeometryZoomRange() {
        assertBadRequest(given().queryParam("zoom", "12"), "bbox query parameter is required");
        assertBadRequest(given().queryParam("bbox", BBOX), "zoom query parameter is required");
        assertBadRequest(request(BBOX, "11"), "requires zoom between 12 and 22");
        assertBadRequest(request(BBOX, "23"), "requires zoom between 12 and 22");
        assertBadRequest(request(BBOX, "12.5"), "zoom must be an integer");
        assertBadRequest(request("170,45,-170,47", "12"), "Antimeridian-crossing");

        request(BBOX, "12").get("/api/tours/geometry").then().statusCode(200);
        request(BBOX, "22").get("/api/tours/geometry").then().statusCode(200);
    }

    @Test
    void returnsValidPublishedLineStringWithoutInternalData() {
        long published = createPublishedTour(
                "Published route",
                "SI",
                TechnicalDifficulty.S2,
                "20.00",
                1_000,
                line(14.0, 46.0),
                PUBLICATION_TIME
        );
        createDraftTour("Draft route", line(14.1, 46.0));
        createPublishedTour(
                "Outside route",
                "SI",
                TechnicalDifficulty.S3,
                "20.00",
                1_000,
                line(16.0, 46.0),
                PUBLICATION_TIME.minusHours(1)
        );

        Response response = request(BBOX, "22").get("/api/tours/geometry");

        response.then()
                .statusCode(200)
                .body("type", equalTo("FeatureCollection"))
                .body("metadata.zoom", equalTo(22))
                .body("metadata.tourCount", equalTo(1))
                .body("metadata.vertexCount", equalTo(2))
                .body("features", hasSize(1))
                .body("features[0].type", equalTo("Feature"))
                .body("features[0].geometry.type", equalTo("LineString"))
                .body("features[0].properties.tourId", equalTo((int) published));

        List<Number> coordinate = response.jsonPath().getList(
                "features[0].geometry.coordinates[0]"
        );
        assertTrue(coordinate.get(0).doubleValue() >= 13.0);
        assertTrue(coordinate.get(0).doubleValue() <= 15.0);
        assertTrue(coordinate.get(1).doubleValue() >= 45.0);
        assertTrue(coordinate.get(1).doubleValue() <= 47.0);

        String body = response.asString();
        assertFalse(body.contains("Draft route"));
        assertFalse(body.contains("private/"));
        assertFalse(body.contains("gpx"));
        assertFalse(body.contains("elevationProfile"));
    }

    @Test
    void returnsEmptyFeatureCollectionForEmptyViewport() {
        request("10,40,11,41", "13")
                .get("/api/tours/geometry")
                .then()
                .statusCode(200)
                .body("metadata.tourCount", equalTo(0))
                .body("metadata.vertexCount", equalTo(0))
                .body("features", hasSize(0));
    }

    @Test
    void originalBboxControlsMembershipWhilePaddingOnlyControlsClipping() {
        long boundary = createPublishedTour(
                "Boundary route",
                "SI",
                TechnicalDifficulty.S2,
                "20.00",
                1_000,
                line(13.0, 46.0),
                PUBLICATION_TIME
        );
        createPublishedTour(
                "Only in padding",
                "SI",
                TechnicalDifficulty.S2,
                "20.00",
                1_000,
                line(15.1, 46.0),
                PUBLICATION_TIME.minusHours(1)
        );

        Response response = request(BBOX, "22").get("/api/tours/geometry");

        response.then().statusCode(200).body("features", hasSize(1));
        assertEquals(Set.of(boundary), geometryTourIds(response));
    }

    @Test
    void listMapAndGeometryUseIdenticalFilterMembership() {
        long first = createPublishedTour(
                "Matching one", "SI", TechnicalDifficulty.S2,
                "20.00", 1_000, line(14.0, 46.0), PUBLICATION_TIME
        );
        long second = createPublishedTour(
                "Matching two", "SI", TechnicalDifficulty.S3,
                "40.00", 2_000, line(14.2, 46.0), PUBLICATION_TIME.minusHours(1)
        );
        createPublishedTour(
                "Wrong country", "IT", TechnicalDifficulty.S2,
                "20.00", 1_000, line(14.3, 46.0), PUBLICATION_TIME.minusHours(2)
        );
        createPublishedTour(
                "Wrong difficulty", "SI", TechnicalDifficulty.S4,
                "20.00", 1_000, line(14.4, 46.0), PUBLICATION_TIME.minusHours(3)
        );

        Response list = filtered(given().queryParam("bbox", BBOX)).get("/api/tours");
        Response map = filtered(request(BBOX, "22")).get("/api/tours/map");
        Response geometry = filtered(request(BBOX, "22")).get("/api/tours/geometry");

        Set<Long> expected = Set.of(first, second);
        assertEquals(expected, new HashSet<>(list.jsonPath().getList("items.id", Long.class)));
        assertEquals(expected, new HashSet<>(
                map.jsonPath().getList("features.properties.tourId", Long.class)
        ));
        assertEquals(expected, geometryTourIds(geometry));
    }

    @Test
    void preservesDisconnectedSegmentsAsMultiLineStringAfterClipping() {
        createPublishedTour(
                "Clipped multiple segments",
                "SI",
                TechnicalDifficulty.S3,
                "20.00",
                1_000,
                lineString(
                        new Coordinate(13.8, 46.0),
                        new Coordinate(13.9, 48.0),
                        new Coordinate(14.0, 46.0),
                        new Coordinate(14.1, 48.0),
                        new Coordinate(14.2, 46.0)
                ),
                PUBLICATION_TIME
        );

        request(BBOX, "22")
                .get("/api/tours/geometry")
                .then()
                .statusCode(200)
                .body("features", hasSize(1))
                .body("features[0].geometry.type", equalTo("MultiLineString"));
    }

    @Test
    void omitsDegenerateLineResults() {
        createPublishedTour(
                "Point-only clipped result",
                "SI",
                TechnicalDifficulty.S2,
                "20.00",
                1_000,
                lineString(
                        new Coordinate(179.0, 86.0),
                        new Coordinate(180.0, 85.05112878),
                        new Coordinate(179.0, 86.0)
                ),
                PUBLICATION_TIME
        );

        request("179,84,180,85.05112878", "22")
                .get("/api/tours/geometry")
                .then()
                .statusCode(200)
                .body("metadata.tourCount", equalTo(0))
                .body("features", hasSize(0));
    }

    @Test
    void lowerZoomSimplifiesMoreWithoutChangingStoredGeometry() {
        long tourId = createPublishedTour(
                "Detailed route",
                "SI",
                TechnicalDifficulty.S3,
                "20.00",
                1_000,
                detailedLine(60),
                PUBLICATION_TIME
        );
        String originalGeometry = storedGeometry(tourId);

        Response lowZoom = request(BBOX, "12").get("/api/tours/geometry");
        Response highZoom = request(BBOX, "22").get("/api/tours/geometry");

        lowZoom.then().statusCode(200);
        highZoom.then().statusCode(200);
        lowZoom.then().body(
                "metadata.vertexCount",
                lessThan(highZoom.jsonPath().getInt("metadata.vertexCount"))
        );
        highZoom.then().body("metadata.vertexCount", greaterThan(2));
        assertEquals(originalGeometry, storedGeometry(tourId));
    }

    @Test
    void acceptsFiftyToursAndRejectsFiftyOneWithoutTruncation() {
        for (int index = 0; index < 50; index++) {
            createPublishedTour(
                    "Limit " + index,
                    "SI",
                    TechnicalDifficulty.S2,
                    "20.00",
                    1_000,
                    line(14.0 + (index * 0.001), 46.0),
                    PUBLICATION_TIME.minusMinutes(index)
            );
        }

        request(BBOX, "22")
                .get("/api/tours/geometry")
                .then()
                .statusCode(200)
                .body("metadata.tourCount", equalTo(50))
                .body("metadata.vertexCount", equalTo(100))
                .body("features", hasSize(50));

        createPublishedTour(
                "Limit 51",
                "SI",
                TechnicalDifficulty.S2,
                "20.00",
                1_000,
                line(14.1, 46.0),
                PUBLICATION_TIME.minusHours(2)
        );

        assertDensityError(request(BBOX, "22"), "Too many route geometries");
    }

    @Test
    void rejectsVertexBudgetOverflowWithoutPartialGeometry() {
        createPublishedTour(
                "Vertex overflow",
                "SI",
                TechnicalDifficulty.S3,
                "20.00",
                1_000,
                detailedLine(101),
                PUBLICATION_TIME
        );

        assertDensityError(request(BBOX, "22"), "vertex budget");
    }

    private long createPublishedTour(
            String title,
            String countryCode,
            TechnicalDifficulty difficulty,
            String distanceKm,
            int ascentMeters,
            LineString geometry,
            OffsetDateTime publishedAt
    ) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Tour tour = completeTour(
                    title,
                    countryCode,
                    difficulty,
                    distanceKm,
                    ascentMeters,
                    geometry
            );
            tour.publish(publishedAt);
            repository.persistAndFlush(tour);
            return tour.getId();
        });
    }

    private long createDraftTour(String title, LineString geometry) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Tour tour = completeTour(
                    title,
                    "SI",
                    TechnicalDifficulty.S2,
                    "20.00",
                    1_000,
                    geometry
            );
            repository.persistAndFlush(tour);
            return tour.getId();
        });
    }

    private static Tour completeTour(
            String title,
            String countryCode,
            TechnicalDifficulty difficulty,
            String distanceKm,
            int ascentMeters,
            LineString geometry
    ) {
        Coordinate start = geometry.getCoordinateN(0);
        Tour tour = Tour.createDraft();
        tour.setIdentity(title, "Julian Alps", countryCode);
        tour.setStartLocation(new StartLocation(
                "Start",
                decimal(start.getY()),
                decimal(start.getX())
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
        tour.setRouteGeometry(geometry);
        tour.setDifficulty(difficulty, 3, ExposureRating.E2, 4, 10);
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

    private static LineString line(double midpointLongitude, double midpointLatitude) {
        return lineString(
                new Coordinate(midpointLongitude - 0.01, midpointLatitude),
                new Coordinate(midpointLongitude + 0.01, midpointLatitude)
        );
    }

    private static LineString detailedLine(int pointCount) {
        List<Coordinate> coordinates = new ArrayList<>(pointCount);
        for (int index = 0; index < pointCount; index++) {
            coordinates.add(new Coordinate(
                    13.7 + (index * 0.005),
                    46.0 + (index % 2 == 0 ? -0.0001 : 0.0001)
            ));
        }
        return lineString(coordinates.toArray(Coordinate[]::new));
    }

    private static LineString lineString(Coordinate... coordinates) {
        return GEOMETRY_FACTORY.createLineString(coordinates);
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP);
    }

    private String storedGeometry(long tourId) {
        return QuarkusTransaction.requiringNew().call(() -> (String) entityManager
                .createNativeQuery("SELECT ST_AsEWKT(route_geometry) FROM tour WHERE id = :id")
                .setParameter("id", tourId)
                .getSingleResult());
    }

    private static RequestSpecification request(String bbox, String zoom) {
        return given().queryParam("bbox", bbox).queryParam("zoom", zoom);
    }

    private static RequestSpecification filtered(RequestSpecification request) {
        return request
                .queryParam("countryCode", "si")
                .queryParam("technicalDifficulty", "S2,S3")
                .queryParam("distanceMinKm", "15")
                .queryParam("distanceMaxKm", "45")
                .queryParam("ascentMinMeters", "900")
                .queryParam("ascentMaxMeters", "2100");
    }

    private static Set<Long> geometryTourIds(Response response) {
        return new HashSet<>(response.jsonPath().getList(
                "features.properties.tourId",
                Long.class
        ));
    }

    private static void assertBadRequest(
            RequestSpecification request,
            String messageFragment
    ) {
        Response response = request.get("/api/tours/geometry");
        response.then().statusCode(400);
        assertTrue(response.jsonPath().getString("message").contains(messageFragment));
    }

    private static void assertDensityError(
            RequestSpecification request,
            String messageFragment
    ) {
        Response response = request.get("/api/tours/geometry");
        response.then()
                .statusCode(422)
                .body("code", equalTo("GEOMETRY_TOO_DENSE"))
                .body("message", notNullValue());
        assertTrue(response.jsonPath().getString("message").contains(messageFragment));
        assertFalse(response.asString().contains("coordinates"));
    }
}
