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
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TourPersistenceTest {

    @Inject
    EntityManager entityManager;

    @Inject
    TourRepository repository;

    @Test
    @TestTransaction
    void lineStringAndOwnedImportDataRoundTrip() {
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
    void postgisSchemaObjectsAndOneTrackConstraintExist() {
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

        assertNotNull(extensionVersion);
        assertTrue(spatialIndexCount.longValue() >= 1);
        assertEquals(1, oneTrackConstraintCount.longValue());
    }

    private static LineString lineString() {
        return new GeometryFactory(new PrecisionModel(), 4326).createLineString(new Coordinate[]{
                new Coordinate(13.5321, 46.4776),
                new Coordinate(13.5432, 46.4890)
        });
    }
}
