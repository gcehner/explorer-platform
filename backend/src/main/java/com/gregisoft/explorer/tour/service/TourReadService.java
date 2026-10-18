package com.gregisoft.explorer.tour.service;

import com.gregisoft.explorer.tour.api.DurationRangeResponse;
import com.gregisoft.explorer.tour.api.ElevationProfileSampleResponse;
import com.gregisoft.explorer.tour.api.GeoJsonLineStringResponse;
import com.gregisoft.explorer.tour.api.PhotoResponse;
import com.gregisoft.explorer.tour.api.StartLocationResponse;
import com.gregisoft.explorer.tour.api.SurfaceShareResponse;
import com.gregisoft.explorer.tour.api.TourDetailResponse;
import com.gregisoft.explorer.tour.api.TourStatisticsResponse;
import com.gregisoft.explorer.tour.entity.Photo;
import com.gregisoft.explorer.tour.entity.Tour;
import com.gregisoft.explorer.tour.repository.TourRepository;
import com.gregisoft.explorer.tour.type.BikeType;
import com.gregisoft.explorer.tour.value.DurationRange;
import com.gregisoft.explorer.tour.value.StartLocation;
import com.gregisoft.explorer.tour.value.TourStatistics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class TourReadService {

    private final TourRepository repository;

    public TourReadService(TourRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TourDetailResponse getPublishedTour(long id) {
        Tour tour = repository.findPublishedById(id)
                .orElseThrow(() -> new NotFoundException("Tour was not found."));
        return toResponse(tour);
    }

    @Transactional
    public TourDetailResponse getManagementTour(long id) {
        Tour tour = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tour was not found."));
        return toResponse(tour);
    }

    private static TourDetailResponse toResponse(Tour tour) {
        Photo coverPhoto = tour.getCoverPhoto();
        return new TourDetailResponse(
                tour.getId(),
                tour.getTitle(),
                tour.getMountainRange(),
                tour.getCountryCode(),
                toResponse(tour.getStartLocation()),
                tour.getShortDescription(),
                tour.getTechnicalDescription(),
                toResponse(tour.getStatistics()),
                toResponse(tour.getEstimatedDuration()),
                tour.getTourDate(),
                tour.getTechnicalDifficulty(),
                tour.getPhysicalDifficulty(),
                tour.getExposureRating(),
                tour.getScenicRating(),
                tour.getCarryingPercentage(),
                tour.getRouteType(),
                tour.getRecommendedBikeTypes().stream()
                        .sorted(Comparator.comparingInt(BikeType::ordinal))
                        .toList(),
                tour.getSurfaceComposition().stream()
                        .map(share -> new SurfaceShareResponse(
                                share.getSurfaceType(),
                                share.getPercentage()
                        ))
                        .toList(),
                tour.getPublicationStatus(),
                tour.getPublishedAt(),
                tour.getElevationProfile().stream()
                        .map(sample -> new ElevationProfileSampleResponse(
                                sample.getDistanceKm(),
                                sample.getElevationMeters()
                        ))
                        .toList(),
                toResponse(tour.getRouteGeometry()),
                tour.getPhotos().stream()
                        .map(photo -> new PhotoResponse(
                                photo.getId(),
                                photo.getImageReference(),
                                photo.getCaption(),
                                photo.getDisplayOrder()
                        ))
                        .toList(),
                coverPhoto == null ? null : coverPhoto.getId(),
                tour.getYoutubeVideoId()
        );
    }

    private static StartLocationResponse toResponse(StartLocation startLocation) {
        if (startLocation == null) {
            return null;
        }
        return new StartLocationResponse(
                startLocation.getName(),
                startLocation.getLatitude(),
                startLocation.getLongitude()
        );
    }

    private static TourStatisticsResponse toResponse(TourStatistics statistics) {
        if (statistics == null) {
            return null;
        }
        return new TourStatisticsResponse(
                statistics.getDistanceKm(),
                statistics.getTotalAscentMeters(),
                statistics.getTotalDescentMeters(),
                statistics.getLowestPointMeters(),
                statistics.getHighestPointMeters()
        );
    }

    private static DurationRangeResponse toResponse(DurationRange duration) {
        if (duration == null) {
            return null;
        }
        return new DurationRangeResponse(duration.getMinimumHours(), duration.getMaximumHours());
    }

    private static GeoJsonLineStringResponse toResponse(LineString geometry) {
        if (geometry == null) {
            return null;
        }
        List<List<BigDecimal>> coordinates = Arrays.stream(geometry.getCoordinates())
                .map(TourReadService::toPosition)
                .toList();
        return new GeoJsonLineStringResponse("LineString", coordinates);
    }

    private static List<BigDecimal> toPosition(Coordinate coordinate) {
        return List.of(
                BigDecimal.valueOf(coordinate.getX()),
                BigDecimal.valueOf(coordinate.getY())
        );
    }
}
