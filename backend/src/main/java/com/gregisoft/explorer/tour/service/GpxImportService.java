package com.gregisoft.explorer.tour.service;

import com.gregisoft.explorer.tour.api.ElevationProfileSampleRequest;
import com.gregisoft.explorer.tour.api.GeoJsonLineStringRequest;
import com.gregisoft.explorer.tour.api.GpxImportMetadataRequest;
import com.gregisoft.explorer.tour.api.GpxImportResponse;
import com.gregisoft.explorer.tour.entity.GpxTrack;
import com.gregisoft.explorer.tour.entity.Tour;
import com.gregisoft.explorer.tour.repository.TourRepository;
import com.gregisoft.explorer.tour.storage.GpxSourceStorage;
import com.gregisoft.explorer.tour.type.ImportSourceProvider;
import com.gregisoft.explorer.tour.value.ElevationProfileSample;
import com.gregisoft.explorer.tour.value.TourStatistics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@ApplicationScoped
public class GpxImportService {

    private static final int WGS_84_SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), WGS_84_SRID);
    private static final Pattern GARMIN_ACTIVITY_ID = Pattern.compile("[1-9][0-9]*");

    private final TourRepository repository;
    private final GpxSourceStorage storage;

    public GpxImportService(TourRepository repository, GpxSourceStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional
    public GpxImportResult importGpx(
            GpxImportMetadataRequest request,
            Path uploadedFile,
            String originalFilename
    ) {
        requireRequest(request);
        requireOriginalFilename(originalFilename);
        ImportSourceIdentity sourceIdentity = parseSourceIdentity(request);

        if (sourceIdentity != null) {
            repository.lockImportSource(sourceIdentity.provider(), sourceIdentity.activityId());
            Optional<Tour> existing = repository.findByImportSource(
                    sourceIdentity.provider(), sourceIdentity.activityId());
            if (existing.isPresent()) {
                Tour existingTour = existing.get();
                return new GpxImportResult(
                        new GpxImportResponse(
                                existingTour.getId(), existingTour.getPublicationStatus()),
                        false
                );
            }
        }

        TourStatistics statistics = createStatistics(request);
        List<ElevationProfileSample> elevationProfile = createElevationProfile(request);
        LineString routeGeometry = createRouteGeometry(request.routeGeometry());

        Tour tour = Tour.createDraft();
        try {
            tour.setStatistics(statistics);
            if (request.tourDate() != null) {
                tour.setTourDate(request.tourDate());
            }
            tour.setElevationProfile(elevationProfile);
            tour.setRouteGeometry(routeGeometry);
        } catch (IllegalArgumentException exception) {
            throw new InvalidGpxImportException(exception.getMessage(), exception);
        }

        String sourceReference = storage.store(uploadedFile);
        tour.attachGpxTrack(new GpxTrack(
                originalFilename,
                sourceReference,
                sourceIdentity == null ? null : sourceIdentity.provider(),
                sourceIdentity == null ? null : sourceIdentity.activityId()
        ));

        try {
            repository.persistAndFlush(tour);
        } catch (RuntimeException exception) {
            try {
                storage.delete(sourceReference);
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
        return new GpxImportResult(
                new GpxImportResponse(tour.getId(), tour.getPublicationStatus()), true);
    }

    @Transactional
    public Optional<GpxImportResponse> findBySource(String provider, String activityId) {
        ImportSourceIdentity sourceIdentity = parseRequiredSourceIdentity(provider, activityId);
        return repository.findByImportSource(sourceIdentity.provider(), sourceIdentity.activityId())
                .map(tour -> new GpxImportResponse(tour.getId(), tour.getPublicationStatus()));
    }

    private static void requireRequest(GpxImportMetadataRequest request) {
        if (request == null) {
            throw new InvalidGpxImportException("Import metadata is required.");
        }
        if (request.distanceKm() == null
                || request.totalAscentMeters() == null
                || request.totalDescentMeters() == null
                || request.lowestPointMeters() == null
                || request.highestPointMeters() == null
                || request.elevationProfile() == null
                || request.routeGeometry() == null) {
            throw new InvalidGpxImportException("All normalized GPX fields except tourDate are required.");
        }
    }

    private static void requireOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidGpxImportException("The original GPX filename is required.");
        }
        if (originalFilename.trim().length() > 255) {
            throw new InvalidGpxImportException("The original GPX filename must not exceed 255 characters.");
        }
    }

    private static ImportSourceIdentity parseSourceIdentity(GpxImportMetadataRequest request) {
        if (request.sourceProvider() == null && request.sourceActivityId() == null) {
            return null;
        }
        if (request.sourceProvider() == null || request.sourceActivityId() == null) {
            throw new InvalidGpxImportException(
                    "Import source provider and activity ID must either both be present or both be absent."
            );
        }
        return parseRequiredSourceIdentity(request.sourceProvider(), request.sourceActivityId());
    }

    private static ImportSourceIdentity parseRequiredSourceIdentity(
            String provider,
            String activityId
    ) {
        ImportSourceProvider parsedProvider;
        try {
            parsedProvider = ImportSourceProvider.valueOf(requireText(provider, "Source provider")
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidGpxImportException("Unsupported import source provider.", exception);
        }
        String normalizedActivityId = requireText(activityId, "Source activity ID");
        if (parsedProvider == ImportSourceProvider.GARMIN_CONNECT
                && !GARMIN_ACTIVITY_ID.matcher(normalizedActivityId).matches()) {
            throw new InvalidGpxImportException(
                    "Garmin activity ID must be a canonical positive decimal string."
            );
        }
        if (normalizedActivityId.length() > 255) {
            throw new InvalidGpxImportException("Source activity ID must not exceed 255 characters.");
        }
        return new ImportSourceIdentity(parsedProvider, normalizedActivityId);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidGpxImportException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private static TourStatistics createStatistics(GpxImportMetadataRequest request) {
        try {
            TourStatistics statistics = new TourStatistics(
                    request.distanceKm(),
                    request.totalAscentMeters(),
                    request.totalDescentMeters(),
                    request.lowestPointMeters(),
                    request.highestPointMeters()
            );
            if (statistics.getDistanceKm().precision() > 8) {
                throw new InvalidGpxImportException("Distance must fit NUMERIC(8,2).");
            }
            return statistics;
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new InvalidGpxImportException(exception.getMessage(), exception);
        }
    }

    private static List<ElevationProfileSample> createElevationProfile(
            GpxImportMetadataRequest request
    ) {
        if (request.elevationProfile().isEmpty()) {
            throw new InvalidGpxImportException("Elevation profile must not be empty.");
        }

        List<ElevationProfileSample> samples = new ArrayList<>(request.elevationProfile().size());
        try {
            for (ElevationProfileSampleRequest sample : request.elevationProfile()) {
                if (sample == null || sample.distanceKm() == null || sample.elevationMeters() == null) {
                    throw new InvalidGpxImportException(
                            "Every elevation profile sample requires distanceKm and elevationMeters."
                    );
                }
                BigDecimal persistedDistance;
                try {
                    persistedDistance = sample.distanceKm().setScale(3, RoundingMode.UNNECESSARY);
                } catch (ArithmeticException exception) {
                    throw new InvalidGpxImportException(
                            "Elevation profile distances must fit NUMERIC(10,3).",
                            exception
                    );
                }
                if (persistedDistance.precision() > 10) {
                    throw new InvalidGpxImportException(
                            "Elevation profile distances must fit NUMERIC(10,3)."
                    );
                }
                samples.add(new ElevationProfileSample(
                        sample.distanceKm(),
                        sample.elevationMeters()
                ));
            }

            return samples;
        } catch (InvalidGpxImportException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new InvalidGpxImportException(exception.getMessage(), exception);
        }
    }

    private static LineString createRouteGeometry(GeoJsonLineStringRequest geometryRequest) {
        if (!"LineString".equals(geometryRequest.type())) {
            throw new InvalidGpxImportException("Route geometry type must be LineString.");
        }
        if (geometryRequest.coordinates() == null || geometryRequest.coordinates().size() < 2) {
            throw new InvalidGpxImportException("Route geometry requires at least two coordinates.");
        }

        Coordinate[] coordinates = new Coordinate[geometryRequest.coordinates().size()];
        for (int index = 0; index < geometryRequest.coordinates().size(); index++) {
            List<BigDecimal> position = geometryRequest.coordinates().get(index);
            if (position == null || position.size() != 2
                    || position.get(0) == null || position.get(1) == null) {
                throw new InvalidGpxImportException(
                        "Every route position must contain exactly longitude and latitude."
                );
            }
            double longitude = position.get(0).doubleValue();
            double latitude = position.get(1).doubleValue();
            if (!Double.isFinite(longitude) || !Double.isFinite(latitude)) {
                throw new InvalidGpxImportException("Route coordinates must be finite.");
            }
            coordinates[index] = new Coordinate(longitude, latitude);
        }

        return GEOMETRY_FACTORY.createLineString(coordinates);
    }

    private record ImportSourceIdentity(
            ImportSourceProvider provider,
            String activityId
    ) {
    }
}
