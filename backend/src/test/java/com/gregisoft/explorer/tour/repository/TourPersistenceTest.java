package com.gregisoft.explorer.tour.repository;

import com.gregisoft.explorer.tour.entity.GpxTrack;
import com.gregisoft.explorer.tour.entity.Tour;
import com.gregisoft.explorer.tour.value.ElevationProfileSample;
import com.gregisoft.explorer.tour.value.TourStatistics;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.gregisoft.explorer.test.TestDatabaseSafety.assertUsingIsolatedTestDatabase;

@QuarkusTest
class TourPersistenceTest {

    @Inject
    EntityManager entityManager;

    @Inject
    TourRepository repository;

    @Test
    @TestTransaction
    void lineStringAndOwnedImportDataRoundTrip() {
        assertUsingIsolatedTestDatabase(entityManager);
        Tour tour = Tour.createDraft();
        tour.setStatistics(new TourStatistics(new BigDecimal("8.25"), 420, 415, 520, 910));
        tour.setElevationProfile(List.of(
                new ElevationProfileSample(new BigDecimal("0.000"), 520),
                new ElevationProfileSample(new BigDecimal("8.250"), 525)
        ));
        tour.setRouteGeometry(lineString());
        tour.attachGpxTrack(new GpxTrack("route.gpx", "generated.gpx"));

        repository.persistAndFlush(tour);
        Long id = tour.getId();
        entityManager.clear();

        Tour reloaded = entityManager.find(Tour.class, id);
        LineString geometry = reloaded.getRouteGeometry();
        assertNotNull(geometry);
        assertEquals(4326, geometry.getSRID());
        assertEquals(2, geometry.getNumPoints());
        assertEquals(13.5321, geometry.getCoordinateN(0).getX(), 0.0000001);
        assertEquals(List.of(new BigDecimal("0.000"), new BigDecimal("8.250")),
                reloaded.getElevationProfile().stream()
                        .map(ElevationProfileSample::getDistanceKm)
                        .toList());
        assertEquals("generated.gpx", reloaded.getGpxTrack().getSourceReference());
    }

    @Test
    @TestTransaction
    void discoveryPointIsGeneratedAtRouteMidpointAndTracksRouteChanges() {
        assertUsingIsolatedTestDatabase(entityManager);
        Tour withRoute = Tour.createDraft();
        withRoute.setRouteGeometry(new GeometryFactory(new PrecisionModel(), 4326)
                .createLineString(new Coordinate[]{
                        new Coordinate(13.0, 46.0),
                        new Coordinate(15.0, 46.0)
                }));
        repository.persistAndFlush(withRoute);

        Object[] generated = discoveryPoint(withRoute.getId());
        assertEquals(14.0, ((Number) generated[0]).doubleValue(), 0.0000001);
        assertEquals(46.0, ((Number) generated[1]).doubleValue(), 0.0000001);

        entityManager.createNativeQuery("""
                update tour
                set route_geometry = ST_GeomFromText('LINESTRING(16 47, 18 47)', 4326)
                where id = :id
                """)
                .setParameter("id", withRoute.getId())
                .executeUpdate();
        entityManager.flush();

        Object[] updated = discoveryPoint(withRoute.getId());
        assertEquals(17.0, ((Number) updated[0]).doubleValue(), 0.0000001);
        assertEquals(47.0, ((Number) updated[1]).doubleValue(), 0.0000001);

        Tour withoutRoute = Tour.createDraft();
        repository.persistAndFlush(withoutRoute);
        Object discoveryPoint = entityManager.createNativeQuery(
                        "select discovery_point from tour where id = :id"
                )
                .setParameter("id", withoutRoute.getId())
                .getSingleResult();
        assertNull(discoveryPoint);
    }

    @Test
    @TestTransaction
    void postgisSchemaObjectsAndOneTrackConstraintExist() {
        assertUsingIsolatedTestDatabase(entityManager);
        Object extensionVersion = entityManager.createNativeQuery(
                "select extversion from pg_extension where extname = 'postgis'"
        ).getSingleResult();
        Number spatialIndexCount = (Number) entityManager.createNativeQuery(
                "select count(*) from pg_indexes "
                        + "where tablename = 'tour' and indexname = 'idx_tour_route_geometry' "
                        + "and indexdef like '%USING gist%route_geometry%'"
        ).getSingleResult();
        Number oneTrackConstraintCount = (Number) entityManager.createNativeQuery(
                "select count(*) from pg_constraint "
                        + "where conrelid = 'gpx_track'::regclass and contype = 'u' "
                        + "and pg_get_constraintdef(oid) = 'UNIQUE (tour_id)'"
        ).getSingleResult();
        Number discoveryPointIndexCount = (Number) entityManager.createNativeQuery(
                "select count(*) from pg_indexes "
                        + "where tablename = 'tour' "
                        + "and indexname = 'idx_tour_published_discovery_point' "
                        + "and indexdef like '%USING gist%discovery_point%' "
                        + "and indexdef like '%publication_status%PUBLISHED%'"
        ).getSingleResult();
        Number publishedOrderIndexCount = (Number) entityManager.createNativeQuery(
                "select count(*) from pg_indexes "
                        + "where tablename = 'tour' "
                        + "and indexname = 'idx_tour_published_order' "
                        + "and indexdef like '%published_at DESC, id DESC%' "
                        + "and indexdef like '%publication_status%PUBLISHED%'"
        ).getSingleResult();
        String generatedColumn = (String) entityManager.createNativeQuery(
                "select is_generated from information_schema.columns "
                        + "where table_name = 'tour' and column_name = 'discovery_point'"
        ).getSingleResult();

        assertNotNull(extensionVersion);
        assertTrue(spatialIndexCount.longValue() >= 1);
        assertEquals(1, oneTrackConstraintCount.longValue());
        assertEquals(1, discoveryPointIndexCount.longValue());
        assertEquals(1, publishedOrderIndexCount.longValue());
        assertEquals("ALWAYS", generatedColumn);
    }

    private Object[] discoveryPoint(Long id) {
        return (Object[]) entityManager.createNativeQuery(
                        "select ST_X(discovery_point), ST_Y(discovery_point) "
                                + "from tour where id = :id"
                )
                .setParameter("id", id)
                .getSingleResult();
    }

    private static LineString lineString() {
        return new GeometryFactory(new PrecisionModel(), 4326).createLineString(new Coordinate[]{
                new Coordinate(13.5321, 46.4776),
                new Coordinate(13.5432, 46.4890)
        });
    }
}
