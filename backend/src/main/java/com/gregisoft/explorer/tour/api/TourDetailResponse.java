package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.type.BikeType;
import com.gregisoft.explorer.tour.type.ExposureRating;
import com.gregisoft.explorer.tour.type.PublicationStatus;
import com.gregisoft.explorer.tour.type.RouteType;
import com.gregisoft.explorer.tour.type.TechnicalDifficulty;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record TourDetailResponse(
        Long id,
        String title,
        String mountainRange,
        String countryCode,
        StartLocationResponse startLocation,
        String shortDescription,
        String technicalDescription,
        TourStatisticsResponse statistics,
        DurationRangeResponse estimatedDuration,
        LocalDate tourDate,
        TechnicalDifficulty technicalDifficulty,
        Integer physicalDifficulty,
        ExposureRating exposureRating,
        Integer scenicRating,
        Integer carryingPercentage,
        RouteType routeType,
        List<BikeType> recommendedBikeTypes,
        List<SurfaceShareResponse> surfaceComposition,
        PublicationStatus publicationStatus,
        OffsetDateTime publishedAt,
        List<ElevationProfileSampleResponse> elevationProfile,
        GeoJsonLineStringResponse routeGeometry,
        List<PhotoResponse> photos,
        Long coverPhotoId,
        String youtubeVideoId
) {
}
