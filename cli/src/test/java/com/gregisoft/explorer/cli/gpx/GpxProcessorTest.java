package com.gregisoft.explorer.cli.gpx;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpxProcessorTest {

    private final GpxProcessor processor = new GpxProcessor();

    @Test
    void calculatesKnownWgs84HorizontalDistance() {
        NormalizedGpxData result = process(segment(
                point(0.0, 0.0, 100.0),
                point(0.0, 0.01, 100.0)));

        assertEquals(new BigDecimal("1.11"), result.distanceKm());
    }

    @Test
    void monotonicEightMeterClimbProducesExactlyEightMetersAscent() {
        NormalizedGpxData result = process(elevationSegment(100, 102, 104, 106, 108));

        assertEquals(8, result.totalAscentMeters());
        assertEquals(0, result.totalDescentMeters());
    }

    @Test
    void monotonicDescentIsCommittedAtEndOfSegment() {
        NormalizedGpxData result = process(elevationSegment(108, 106, 104, 102, 100));

        assertEquals(0, result.totalAscentMeters());
        assertEquals(8, result.totalDescentMeters());
    }

    @Test
    void subThreeMeterNoiseDoesNotProduceGainOrLoss() {
        NormalizedGpxData result = process(elevationSegment(100, 101, 99, 101, 100));

        assertEquals(0, result.totalAscentMeters());
        assertEquals(0, result.totalDescentMeters());
    }

    @Test
    void climbFollowedByThreeMeterReversalCommitsTurningPoint() {
        NormalizedGpxData result = process(elevationSegment(
                100, 102, 104, 106, 108, 108, 108, 106, 104));

        assertEquals(8, result.totalAscentMeters());
        assertEquals(4, result.totalDescentMeters());
    }

    @Test
    void reversalBelowThreeMetersDoesNotSplitClimb() {
        NormalizedGpxData result = process(elevationSegment(
                100, 102, 104, 106, 108, 107, 108, 110, 112));

        assertEquals(12, result.totalAscentMeters());
        assertEquals(0, result.totalDescentMeters());
    }

    @Test
    void segmentBoundariesDoNotAddVerticalGainOrLoss() {
        NormalizedGpxData result = processor.process(new GpxTrackData(List.of(
                segment(point(46.0, 14.0, 100.0), point(46.0, 14.0001, 108.0)),
                segment(point(46.0, 14.0002, 200.0), point(46.0, 14.0003, 192.0)))));

        assertEquals(8, result.totalAscentMeters());
        assertEquals(8, result.totalDescentMeters());
    }

    @Test
    void interpolatesMissingInteriorElevationByHorizontalDistance() {
        NormalizedGpxData result = process(segment(
                point(0.0, 0.0, 100.0),
                point(0.0, 0.005, null),
                point(0.0, 0.01, 110.0)));

        assertEquals(100, result.elevationProfile().getFirst().elevationMeters());
        assertEquals(110, result.elevationProfile().getLast().elevationMeters());
        assertTrue(result.elevationProfile().stream()
                .anyMatch(sample -> sample.elevationMeters() == 105));
    }

    @Test
    void rejectsSegmentWithFewerThanTwoRecordedElevations() {
        GpxTrackData track = new GpxTrackData(List.of(segment(
                point(46.0, 14.0, 100.0),
                point(46.0, 14.001, null))));

        assertThrows(GpxProcessingException.class, () -> processor.process(track));
    }

    @Test
    void includesContinuousSegmentBridgeInDistanceAndGeometry() {
        NormalizedGpxData result = processor.process(new GpxTrackData(List.of(
                segment(point(0.0, 0.0, 100.0), point(0.0, 0.00018, 100.0)),
                segment(point(0.0, 0.00054, 500.0), point(0.0, 0.00126, 500.0)))));

        assertEquals(new BigDecimal("0.14"), result.distanceKm());
        assertEquals(4, result.routeCoordinates().size());
        assertEquals(List.of(new BigDecimal("0.0001800"), new BigDecimal("0.0000000")),
                result.routeCoordinates().get(1));
        assertEquals(List.of(new BigDecimal("0.0005400"), new BigDecimal("0.0000000")),
                result.routeCoordinates().get(2));
        assertEquals(0, result.totalAscentMeters());
        assertEquals(0, result.totalDescentMeters());
    }

    @Test
    void elevationProfileUsesStepAtContinuousSegmentBoundary() {
        NormalizedGpxData result = processor.process(new GpxTrackData(List.of(
                segment(point(0.0, 0.0, 100.0), point(0.0, 0.00018, 100.0)),
                segment(point(0.0, 0.00054, 500.0), point(0.0, 0.00126, 500.0)))));

        assertEquals(4, result.elevationProfile().size());
        assertEquals(100, result.elevationProfile().get(1).elevationMeters());
        assertEquals(500, result.elevationProfile().get(2).elevationMeters());
    }

    @Test
    void rejectsDiscontinuousSegments() {
        GpxTrackData track = new GpxTrackData(List.of(
                segment(point(46.0, 14.0, 100.0), point(46.0, 14.0001, 100.0)),
                segment(point(46.0, 14.01, 100.0), point(46.0, 14.011, 100.0))));

        assertThrows(GpxProcessingException.class, () -> processor.process(track));
    }

    @Test
    void tourDateIsNullWhenTimestampsAreMissing() {
        assertNull(process(elevationSegment(100, 101)).tourDate());
    }

    @Test
    void tourDateUsesEarliestTimestampConvertedToUtc() {
        TrackSegmentData segment = segment(
                new TrackPoint(46.0, 14.0, 100.0, Instant.parse("2025-07-13T00:30:00Z")),
                new TrackPoint(46.0, 14.001, 101.0, Instant.parse("2025-07-12T23:30:00Z")));

        assertEquals(LocalDate.of(2025, 7, 12), process(segment).tourDate());
    }

    @Test
    void profilePreservesFirstAndFinalSamples() {
        NormalizedGpxData result = process(segment(
                point(0.0, 0.0, 100.0),
                point(0.0, 0.001, 110.0)));

        assertEquals(new BigDecimal("0.000"), result.elevationProfile().getFirst().distanceKm());
        assertEquals(new BigDecimal("0.111"), result.elevationProfile().getLast().distanceKm());
        assertEquals(100, result.elevationProfile().getFirst().elevationMeters());
        assertEquals(110, result.elevationProfile().getLast().elevationMeters());
    }

    @Test
    void profileUsesApproximatelyFiftyMeterSpacingAndCapsAtFiveHundredSamples() {
        NormalizedGpxData shortResult = process(segment(
                point(0.0, 0.0, 100.0), point(0.0, 0.001, 110.0)));
        NormalizedGpxData longResult = process(segment(
                point(0.0, 0.0, 100.0), point(0.0, 1.0, 110.0)));

        assertEquals(4, shortResult.elevationProfile().size());
        assertEquals(500, longResult.elevationProfile().size());
    }

    @Test
    void geometryUsesLongitudeLatitudeOrderAndRemovesRoundedDuplicates() {
        NormalizedGpxData result = process(segment(
                point(46.12345674, 14.76543214, 100.0),
                point(46.123456741, 14.765432141, 101.0),
                point(46.12345784, 14.76543324, 102.0)));

        assertEquals(2, result.routeCoordinates().size());
        assertEquals(List.of(new BigDecimal("14.7654321"), new BigDecimal("46.1234567")),
                result.routeCoordinates().getFirst());
    }

    @Test
    void rejectsGeometryCollapsedBySevenDecimalRounding() {
        GpxTrackData track = new GpxTrackData(List.of(segment(
                point(46.000000001, 14.000000001, 100.0),
                point(46.000000002, 14.000000002, 101.0))));

        assertThrows(GpxProcessingException.class, () -> processor.process(track));
    }

    private NormalizedGpxData process(TrackSegmentData segment) {
        return processor.process(new GpxTrackData(List.of(segment)));
    }

    private static TrackSegmentData elevationSegment(double... elevations) {
        List<TrackPoint> points = new ArrayList<>();
        for (int index = 0; index < elevations.length; index++) {
            points.add(point(46.0, 14.0 + index * 0.0001, elevations[index]));
        }
        return new TrackSegmentData(points);
    }

    private static TrackSegmentData segment(TrackPoint... points) {
        return new TrackSegmentData(List.of(points));
    }

    private static TrackPoint point(double latitude, double longitude, Double elevation) {
        return new TrackPoint(latitude, longitude, elevation, null);
    }
}
