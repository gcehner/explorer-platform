package com.gregisoft.explorer.cli.gpx;

import io.jenetics.jpx.WayPoint;
import io.jenetics.jpx.geom.Geoid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GpxProcessor {

    private static final double MAX_SEGMENT_JOIN_METERS = 50.0;
    private static final double ELEVATION_HYSTERESIS_METERS = 3.0;
    private static final double PROFILE_SPACING_METERS = 50.0;
    private static final int MAX_PROFILE_SAMPLES = 500;

    public NormalizedGpxData process(GpxTrackData track) {
        List<RoutePoint> route = new ArrayList<>();
        List<List<BigDecimal>> geometry = new ArrayList<>();
        double totalDistanceMeters = 0.0;
        double totalAscentMeters = 0.0;
        double totalDescentMeters = 0.0;
        double lowestElevationMeters = Double.POSITIVE_INFINITY;
        double highestElevationMeters = Double.NEGATIVE_INFINITY;
        TrackPoint previousSegmentEnd = null;
        Double previousSegmentEndElevation = null;

        for (TrackSegmentData segment : track.segments()) {
            List<TrackPoint> points = segment.points();
            if (previousSegmentEnd != null) {
                double joinDistance = distanceMeters(previousSegmentEnd, points.getFirst());
                if (joinDistance > MAX_SEGMENT_JOIN_METERS) {
                    throw new GpxProcessingException(
                            "Track segments are separated by more than 50 meters");
                }
                totalDistanceMeters += joinDistance;
                route.add(new RoutePoint(totalDistanceMeters, previousSegmentEndElevation));
            }

            double[] segmentDistances = cumulativeDistances(points);
            double[] processedElevations = medianFilter(
                    interpolateElevations(points, segmentDistances));
            ElevationChange elevationChange = calculateElevationChange(processedElevations);
            totalAscentMeters += elevationChange.ascentMeters();
            totalDescentMeters += elevationChange.descentMeters();

            for (int index = 0; index < points.size(); index++) {
                if (index > 0) {
                    totalDistanceMeters += segmentDistances[index] - segmentDistances[index - 1];
                }
                TrackPoint point = points.get(index);
                double elevation = processedElevations[index];
                route.add(new RoutePoint(totalDistanceMeters, elevation));
                lowestElevationMeters = Math.min(lowestElevationMeters, elevation);
                highestElevationMeters = Math.max(highestElevationMeters, elevation);
                addGeometryCoordinate(geometry, point);
            }
            previousSegmentEnd = points.getLast();
            previousSegmentEndElevation = processedElevations[processedElevations.length - 1];
        }

        if (geometry.size() < 2) {
            throw new GpxProcessingException(
                    "Track geometry must contain at least two distinct coordinates");
        }

        LocalDate tourDate = track.segments().stream()
                .flatMap(segment -> segment.points().stream())
                .map(TrackPoint::timestamp)
                .filter(timestamp -> timestamp != null)
                .min(Comparator.naturalOrder())
                .map(timestamp -> timestamp.atZone(ZoneOffset.UTC).toLocalDate())
                .orElse(null);

        return new NormalizedGpxData(
                decimal(totalDistanceMeters / 1_000.0, 2),
                roundedInteger(totalAscentMeters),
                roundedInteger(totalDescentMeters),
                roundedInteger(lowestElevationMeters),
                roundedInteger(highestElevationMeters),
                tourDate,
                createElevationProfile(route, totalDistanceMeters),
                geometry);
    }

    private static double[] cumulativeDistances(List<TrackPoint> points) {
        double[] distances = new double[points.size()];
        for (int index = 1; index < points.size(); index++) {
            distances[index] = distances[index - 1]
                    + distanceMeters(points.get(index - 1), points.get(index));
        }
        return distances;
    }

    private static double[] interpolateElevations(
            List<TrackPoint> points, double[] cumulativeDistances) {
        List<Integer> knownIndices = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            if (points.get(index).elevationMeters() != null) {
                knownIndices.add(index);
            }
        }
        if (knownIndices.size() < 2) {
            throw new GpxProcessingException(
                    "Every track segment must contain at least two elevation values");
        }

        double[] elevations = new double[points.size()];
        int firstKnown = knownIndices.getFirst();
        for (int index = 0; index <= firstKnown; index++) {
            elevations[index] = points.get(firstKnown).elevationMeters();
        }

        for (int knownPosition = 0; knownPosition < knownIndices.size() - 1; knownPosition++) {
            int left = knownIndices.get(knownPosition);
            int right = knownIndices.get(knownPosition + 1);
            double leftElevation = points.get(left).elevationMeters();
            double rightElevation = points.get(right).elevationMeters();
            double distanceSpan = cumulativeDistances[right] - cumulativeDistances[left];
            for (int index = left; index <= right; index++) {
                double fraction = distanceSpan > 0.0
                        ? (cumulativeDistances[index] - cumulativeDistances[left]) / distanceSpan
                        : (double) (index - left) / (right - left);
                elevations[index] = leftElevation
                        + fraction * (rightElevation - leftElevation);
            }
        }

        int lastKnown = knownIndices.getLast();
        for (int index = lastKnown; index < points.size(); index++) {
            elevations[index] = points.get(lastKnown).elevationMeters();
        }
        return elevations;
    }

    private static double[] medianFilter(double[] elevations) {
        double[] filtered = new double[elevations.length];
        for (int index = 0; index < elevations.length; index++) {
            int radius = Math.min(2, Math.min(index, elevations.length - 1 - index));
            int start = index - radius;
            int end = index + radius;
            double[] window = new double[end - start + 1];
            System.arraycopy(elevations, start, window, 0, window.length);
            java.util.Arrays.sort(window);
            int middle = window.length / 2;
            filtered[index] = window[middle];
        }
        return filtered;
    }

    private static ElevationChange calculateElevationChange(double[] elevations) {
        Trend trend = Trend.NONE;
        double initialMinimum = elevations[0];
        double initialMaximum = elevations[0];
        double turningPoint = elevations[0];
        double extreme = elevations[0];
        double ascent = 0.0;
        double descent = 0.0;

        for (int index = 1; index < elevations.length; index++) {
            double elevation = elevations[index];
            switch (trend) {
                case NONE -> {
                    initialMinimum = Math.min(initialMinimum, elevation);
                    initialMaximum = Math.max(initialMaximum, elevation);
                    if (elevation - initialMinimum >= ELEVATION_HYSTERESIS_METERS) {
                        trend = Trend.UP;
                        turningPoint = initialMinimum;
                        extreme = elevation;
                    } else if (initialMaximum - elevation >= ELEVATION_HYSTERESIS_METERS) {
                        trend = Trend.DOWN;
                        turningPoint = initialMaximum;
                        extreme = elevation;
                    }
                }
                case UP -> {
                    if (elevation > extreme) {
                        extreme = elevation;
                    } else if (extreme - elevation >= ELEVATION_HYSTERESIS_METERS) {
                        ascent += extreme - turningPoint;
                        turningPoint = extreme;
                        trend = Trend.DOWN;
                        extreme = elevation;
                    }
                }
                case DOWN -> {
                    if (elevation < extreme) {
                        extreme = elevation;
                    } else if (elevation - extreme >= ELEVATION_HYSTERESIS_METERS) {
                        descent += turningPoint - extreme;
                        turningPoint = extreme;
                        trend = Trend.UP;
                        extreme = elevation;
                    }
                }
            }
        }

        if (trend == Trend.UP) {
            ascent += extreme - turningPoint;
        } else if (trend == Trend.DOWN) {
            descent += turningPoint - extreme;
        }
        return new ElevationChange(ascent, descent);
    }

    private static List<NormalizedGpxData.ElevationSample> createElevationProfile(
            List<RoutePoint> route, double totalDistanceMeters) {
        int intervalCount = Math.min(
                MAX_PROFILE_SAMPLES - 1,
                Math.max(1, (int) Math.ceil(totalDistanceMeters / PROFILE_SPACING_METERS)));
        List<NormalizedGpxData.ElevationSample> samples = new ArrayList<>(intervalCount + 1);
        int routeIndex = 0;
        for (int sampleIndex = 0; sampleIndex <= intervalCount; sampleIndex++) {
            double targetDistance = sampleIndex == intervalCount
                    ? totalDistanceMeters
                    : totalDistanceMeters * sampleIndex / intervalCount;
            while (routeIndex < route.size() - 2
                    && route.get(routeIndex + 1).distanceMeters() <= targetDistance) {
                routeIndex++;
            }
            RoutePoint left = route.get(routeIndex);
            RoutePoint right = route.get(Math.min(routeIndex + 1, route.size() - 1));
            double span = right.distanceMeters() - left.distanceMeters();
            double elevation = span > 0.0
                    ? left.elevationMeters()
                    + (targetDistance - left.distanceMeters()) / span
                    * (right.elevationMeters() - left.elevationMeters())
                    : right.elevationMeters();
            samples.add(new NormalizedGpxData.ElevationSample(
                    decimal(targetDistance / 1_000.0, 3),
                    roundedInteger(elevation)));
        }
        return List.copyOf(samples);
    }

    private static void addGeometryCoordinate(List<List<BigDecimal>> coordinates, TrackPoint point) {
        List<BigDecimal> coordinate = List.of(
                decimal(point.longitude(), 7),
                decimal(point.latitude(), 7));
        if (coordinates.isEmpty() || !coordinates.getLast().equals(coordinate)) {
            coordinates.add(coordinate);
        }
    }

    private static double distanceMeters(TrackPoint first, TrackPoint second) {
        return Geoid.WGS84.distance(
                WayPoint.of(first.latitude(), first.longitude()),
                WayPoint.of(second.latitude(), second.longitude())).doubleValue();
    }

    private static BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private static int roundedInteger(double value) {
        try {
            return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (ArithmeticException exception) {
            throw new GpxProcessingException(
                    "Processed elevation is outside the supported integer-metre range.",
                    exception);
        }
    }

    private enum Trend {
        NONE,
        UP,
        DOWN
    }

    private record ElevationChange(double ascentMeters, double descentMeters) {
    }

    private record RoutePoint(double distanceMeters, double elevationMeters) {
    }
}
