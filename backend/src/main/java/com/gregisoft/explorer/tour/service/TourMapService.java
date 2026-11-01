package com.gregisoft.explorer.tour.service;

import com.gregisoft.explorer.tour.api.TourMapFeature;
import com.gregisoft.explorer.tour.api.TourMapMetadata;
import com.gregisoft.explorer.tour.api.TourMapPoint;
import com.gregisoft.explorer.tour.api.TourMapResponse;
import com.gregisoft.explorer.tour.repository.TourDiscoveryRepository;
import com.gregisoft.explorer.tour.repository.TourDiscoveryRepository.MapCell;
import com.gregisoft.explorer.tour.repository.TourDiscoveryRepository.MapTourPoint;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TourMapService {

    private static final int MINIMUM_ZOOM = 0;
    private static final int MAXIMUM_ZOOM = 22;
    private static final int INDIVIDUAL_TOUR_ZOOM = 12;
    private static final int SINGLETON_TOUR_ZOOM = 10;
    private static final double WEB_MERCATOR_WORLD_METERS = 40_075_016.68557849;
    private static final double TILE_SIZE_PIXELS = 256.0;

    private final TourDiscoveryRepository repository;
    private final int featureCap;
    private final double targetCellPixels;
    private final int maximumAggregationAttempts;

    public TourMapService(
            TourDiscoveryRepository repository,
            @ConfigProperty(name = "explorer.discovery.map.feature-cap", defaultValue = "1000")
            int featureCap,
            @ConfigProperty(name = "explorer.discovery.map.target-cell-pixels", defaultValue = "72")
            double targetCellPixels,
            @ConfigProperty(name = "explorer.discovery.map.max-aggregation-attempts", defaultValue = "12")
            int maximumAggregationAttempts
    ) {
        if (featureCap < 1 || targetCellPixels <= 0 || maximumAggregationAttempts < 1) {
            throw new IllegalArgumentException("Invalid tour map discovery configuration.");
        }
        this.repository = repository;
        this.featureCap = featureCap;
        this.targetCellPixels = targetCellPixels;
        this.maximumAggregationAttempts = maximumAggregationAttempts;
    }

    @Transactional
    public TourMapResponse findMapFeatures(
            String bbox,
            String zoom,
            String countryCode,
            String technicalDifficulty,
            String distanceMinKm,
            String distanceMaxKm,
            String ascentMinMeters,
            String ascentMaxMeters
    ) {
        TourDiscoveryCriteria criteria = TourDiscoveryCriteriaParser.parse(
                bbox,
                countryCode,
                technicalDifficulty,
                distanceMinKm,
                distanceMaxKm,
                ascentMinMeters,
                ascentMaxMeters
        );
        int resolvedZoom = parseZoom(zoom);

        if (resolvedZoom >= INDIVIDUAL_TOUR_ZOOM
                && repository.countPublishedTourPointsUpTo(criteria, featureCap) <= featureCap) {
            List<MapTourPoint> points = repository.findPublishedTourPoints(criteria);
            if (points.size() <= featureCap) {
                return tourPointResponse(resolvedZoom, points);
            }
        }

        return clusteredResponse(criteria, resolvedZoom);
    }

    private TourMapResponse clusteredResponse(
            TourDiscoveryCriteria criteria,
            int zoom
    ) {
        double cellSizeMeters = initialCellSizeMeters(zoom);
        boolean clustersOnly = zoom < SINGLETON_TOUR_ZOOM
                || zoom >= INDIVIDUAL_TOUR_ZOOM;

        for (int attempt = 0; attempt < maximumAggregationAttempts; attempt++) {
            long featureCount = repository.countMapCellsUpTo(
                    criteria,
                    cellSizeMeters,
                    featureCap
            );
            if (featureCount <= featureCap) {
                List<MapCell> cells = repository.findMapCells(criteria, cellSizeMeters);
                if (cells.size() <= featureCap) {
                    return cellResponse(zoom, cellSizeMeters, attempt, clustersOnly, cells);
                }
            }
            cellSizeMeters *= 2.0;
        }

        throw new TourMapDensityException(
                "The requested map area is too dense to represent safely. "
                        + "Use a smaller bounding box or more restrictive filters."
        );
    }

    private TourMapResponse tourPointResponse(int zoom, List<MapTourPoint> points) {
        List<TourMapFeature> features = points.stream()
                .map(TourMapService::tourFeature)
                .toList();
        return new TourMapResponse(
                "FeatureCollection",
                new TourMapMetadata(zoom, "TOURS", 0.0, 0, features.size(), featureCap),
                features
        );
    }

    private TourMapResponse cellResponse(
            int zoom,
            double cellSizeMeters,
            int aggregationAttempt,
            boolean clustersOnly,
            List<MapCell> cells
    ) {
        List<TourMapFeature> features = new ArrayList<>(cells.size());
        boolean containsCluster = false;
        boolean containsTour = false;
        for (MapCell cell : cells) {
            if (!clustersOnly && cell.count() == 1) {
                features.add(tourFeature(cell));
                containsTour = true;
            } else {
                features.add(clusterFeature(zoom, aggregationAttempt, cell));
                containsCluster = true;
            }
        }
        String mode = mode(clustersOnly, containsCluster, containsTour);
        return new TourMapResponse(
                "FeatureCollection",
                new TourMapMetadata(
                        zoom,
                        mode,
                        cellSizeMeters,
                        aggregationAttempt,
                        features.size(),
                        featureCap
                ),
                features
        );
    }

    private static TourMapFeature tourFeature(MapTourPoint point) {
        return tourFeature(
                point.id(),
                point.title(),
                point.technicalDifficulty().name(),
                point.longitude(),
                point.latitude()
        );
    }

    private static TourMapFeature tourFeature(MapCell cell) {
        return tourFeature(
                cell.tourId(),
                cell.title(),
                cell.technicalDifficulty().name(),
                cell.longitude(),
                cell.latitude()
        );
    }

    private static TourMapFeature tourFeature(
            long tourId,
            String title,
            String technicalDifficulty,
            double longitude,
            double latitude
    ) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("kind", "tour");
        properties.put("tourId", tourId);
        properties.put("title", title);
        properties.put("technicalDifficulty", technicalDifficulty);
        return new TourMapFeature(
                "Feature",
                point(longitude, latitude),
                properties
        );
    }

    private static TourMapFeature clusterFeature(
            int zoom,
            int aggregationAttempt,
            MapCell cell
    ) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("kind", "cluster");
        properties.put("count", cell.count());
        properties.put(
                "clusterKey",
                "z" + zoom + ":a" + aggregationAttempt + ":"
                        + cell.gridX() + ":" + cell.gridY()
        );
        properties.put(
                "bounds",
                List.of(cell.west(), cell.south(), cell.east(), cell.north())
        );
        return new TourMapFeature(
                "Feature",
                point(cell.longitude(), cell.latitude()),
                properties
        );
    }

    private static TourMapPoint point(double longitude, double latitude) {
        return new TourMapPoint("Point", List.of(longitude, latitude));
    }

    private static String mode(
            boolean clustersOnly,
            boolean containsCluster,
            boolean containsTour
    ) {
        if (clustersOnly || !containsTour) {
            return "CLUSTERS";
        }
        return containsCluster ? "MIXED" : "TOURS";
    }

    private double initialCellSizeMeters(int zoom) {
        double metersPerPixel = WEB_MERCATOR_WORLD_METERS
                / (Math.pow(2.0, zoom) * TILE_SIZE_PIXELS);
        return metersPerPixel * targetCellPixels;
    }

    private static int parseZoom(String value) {
        if (value == null || value.isBlank()) {
            throw new TourDiscoveryException("The zoom query parameter is required.");
        }
        try {
            int zoom = Integer.parseInt(value.trim());
            if (zoom < MINIMUM_ZOOM || zoom > MAXIMUM_ZOOM) {
                throw new TourDiscoveryException("zoom must be between 0 and 22.");
            }
            return zoom;
        } catch (NumberFormatException exception) {
            throw new TourDiscoveryException("zoom must be an integer between 0 and 22.");
        }
    }
}
