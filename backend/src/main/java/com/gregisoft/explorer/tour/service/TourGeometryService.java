package com.gregisoft.explorer.tour.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gregisoft.explorer.tour.api.TourGeometryFeature;
import com.gregisoft.explorer.tour.api.TourGeometryMetadata;
import com.gregisoft.explorer.tour.api.TourGeometryResponse;
import com.gregisoft.explorer.tour.repository.TourDiscoveryRepository;
import com.gregisoft.explorer.tour.repository.TourDiscoveryRepository.GeometryProjection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TourGeometryService {

    private static final int MINIMUM_ZOOM = 12;
    private static final int MAXIMUM_ZOOM = 22;
    private static final double WEB_MERCATOR_WORLD_METERS = 40_075_016.68557849;
    private static final double WEB_MERCATOR_MAXIMUM_LATITUDE = 85.05112878;
    private static final double TILE_SIZE_PIXELS = 256.0;

    private final TourDiscoveryRepository repository;
    private final ObjectMapper objectMapper;
    private final int maximumTours;
    private final int maximumVertices;
    private final double viewportPaddingFraction;
    private final double simplificationPixels;

    public TourGeometryService(
            TourDiscoveryRepository repository,
            ObjectMapper objectMapper,
            @ConfigProperty(name = "explorer.discovery.geometry.max-tours", defaultValue = "50")
            int maximumTours,
            @ConfigProperty(name = "explorer.discovery.geometry.max-vertices", defaultValue = "50000")
            int maximumVertices,
            @ConfigProperty(name = "explorer.discovery.geometry.viewport-padding", defaultValue = "0.20")
            double viewportPaddingFraction,
            @ConfigProperty(name = "explorer.discovery.geometry.simplification-pixels", defaultValue = "1.5")
            double simplificationPixels
    ) {
        if (maximumTours < 1
                || maximumVertices < 2
                || viewportPaddingFraction < 0
                || viewportPaddingFraction > 1
                || simplificationPixels <= 0) {
            throw new IllegalArgumentException("Invalid tour geometry discovery configuration.");
        }
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.maximumTours = maximumTours;
        this.maximumVertices = maximumVertices;
        this.viewportPaddingFraction = viewportPaddingFraction;
        this.simplificationPixels = simplificationPixels;
    }

    @Transactional
    public TourGeometryResponse findGeometries(
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
        double toleranceMeters = simplificationToleranceMeters(resolvedZoom);

        if (repository.countPublishedTourPointsUpTo(criteria, maximumTours) > maximumTours) {
            throw densityException("Too many route geometries for this viewport. ");
        }

        PaddedViewport viewport = paddedViewport(criteria.boundingBox());
        if (!viewport.isRenderable()) {
            return emptyResponse(resolvedZoom, toleranceMeters);
        }

        List<GeometryProjection> projections = repository.findPublishedTourGeometries(
                criteria,
                viewport.west(),
                viewport.south(),
                viewport.east(),
                viewport.north(),
                toleranceMeters,
                maximumTours,
                maximumVertices
        );
        if (projections.stream().anyMatch(
                projection -> projection.selectedTourCount() > maximumTours
        )) {
            throw densityException("Too many route geometries for this viewport. ");
        }
        int totalVertices = projections.isEmpty()
                ? 0
                : projections.getFirst().totalVertexCount();
        if (totalVertices > maximumVertices) {
            throw densityException("Simplified route geometries exceed the vertex budget. ");
        }

        List<TourGeometryFeature> features = projections.stream()
                .map(this::toFeature)
                .toList();
        return new TourGeometryResponse(
                "FeatureCollection",
                new TourGeometryMetadata(
                        resolvedZoom,
                        toleranceMeters,
                        features.size(),
                        totalVertices
                ),
                features
        );
    }

    private TourGeometryFeature toFeature(GeometryProjection projection) {
        if (projection.geoJson() == null) {
            throw new IllegalStateException("Renderable geometry was not serialized.");
        }
        try {
            JsonNode geometry = objectMapper.readTree(projection.geoJson());
            return new TourGeometryFeature(
                    "Feature",
                    geometry,
                    Map.of("tourId", projection.tourId())
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("PostGIS returned invalid GeoJSON.", exception);
        }
    }

    private TourGeometryResponse emptyResponse(int zoom, double toleranceMeters) {
        return new TourGeometryResponse(
                "FeatureCollection",
                new TourGeometryMetadata(zoom, toleranceMeters, 0, 0),
                List.of()
        );
    }

    private PaddedViewport paddedViewport(TourDiscoveryCriteria.BoundingBox bbox) {
        double west = bbox.west().doubleValue();
        double south = bbox.south().doubleValue();
        double east = bbox.east().doubleValue();
        double north = bbox.north().doubleValue();
        double longitudePadding = (east - west) * viewportPaddingFraction;
        double latitudePadding = (north - south) * viewportPaddingFraction;
        return new PaddedViewport(
                Math.max(-180.0, west - longitudePadding),
                Math.max(-WEB_MERCATOR_MAXIMUM_LATITUDE, south - latitudePadding),
                Math.min(180.0, east + longitudePadding),
                Math.min(WEB_MERCATOR_MAXIMUM_LATITUDE, north + latitudePadding)
        );
    }

    private double simplificationToleranceMeters(int zoom) {
        double metersPerPixel = WEB_MERCATOR_WORLD_METERS
                / (Math.pow(2.0, zoom) * TILE_SIZE_PIXELS);
        return metersPerPixel * simplificationPixels;
    }

    private static int parseZoom(String value) {
        if (value == null || value.isBlank()) {
            throw new TourDiscoveryException("The zoom query parameter is required.");
        }
        try {
            int zoom = Integer.parseInt(value.trim());
            if (zoom < MINIMUM_ZOOM || zoom > MAXIMUM_ZOOM) {
                throw new TourDiscoveryException(
                        "Geometry discovery requires zoom between 12 and 22."
                );
            }
            return zoom;
        } catch (NumberFormatException exception) {
            throw new TourDiscoveryException("zoom must be an integer between 12 and 22.");
        }
    }

    private static TourGeometryDensityException densityException(String reason) {
        return new TourGeometryDensityException(
                reason + "Display clusters or zoom in."
        );
    }

    private record PaddedViewport(
            double west,
            double south,
            double east,
            double north
    ) {
        boolean isRenderable() {
            return west < east && south < north;
        }
    }
}
