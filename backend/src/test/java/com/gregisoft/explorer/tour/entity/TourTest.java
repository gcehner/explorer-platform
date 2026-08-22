package com.gregisoft.explorer.tour.entity;

import com.gregisoft.explorer.tour.type.BikeType;
import com.gregisoft.explorer.tour.type.ExposureRating;
import com.gregisoft.explorer.tour.type.PublicationStatus;
import com.gregisoft.explorer.tour.type.RouteType;
import com.gregisoft.explorer.tour.type.SurfaceType;
import com.gregisoft.explorer.tour.type.TechnicalDifficulty;
import com.gregisoft.explorer.tour.value.DurationRange;
import com.gregisoft.explorer.tour.value.ElevationProfileSample;
import com.gregisoft.explorer.tour.value.StartLocation;
import com.gregisoft.explorer.tour.value.SurfaceShare;
import com.gregisoft.explorer.tour.value.TourStatistics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TourTest {

    @Test
    void newTourStartsAsIncompleteDraft() {
        Tour tour = Tour.createDraft();

        assertEquals(PublicationStatus.DRAFT, tour.getPublicationStatus());
        assertNull(tour.getPublishedAt());
        assertNull(tour.getTourDate());
    }

    @Test
    void publicationRequiresTourDate() {
        Tour tour = completeDraft(false);

        assertThrows(
                IllegalStateException.class,
                () -> tour.publish(OffsetDateTime.parse("2026-08-22T12:00:00+02:00"))
        );
        assertEquals(PublicationStatus.DRAFT, tour.getPublicationStatus());
    }

    @Test
    void firstPublicationTimeIsPreserved() {
        Tour tour = completeDraft(true);
        OffsetDateTime firstPublication = OffsetDateTime.parse("2026-08-22T12:00:00+02:00");

        tour.publish(firstPublication);
        tour.publish(OffsetDateTime.parse("2026-08-23T12:00:00+02:00"));

        assertEquals(PublicationStatus.PUBLISHED, tour.getPublicationStatus());
        assertEquals(firstPublication, tour.getPublishedAt());
    }

    @Test
    void coverPhotoMustBelongToTour() {
        Tour firstTour = Tour.createDraft();
        Tour secondTour = Tour.createDraft();
        Photo photo = new Photo("photos/mangart.jpg", "Mangart", 0);
        firstTour.addPhoto(photo);

        assertThrows(IllegalArgumentException.class, () -> secondTour.selectCoverPhoto(photo));
    }

    @Test
    void photoDisplayOrderMustBeUniqueWithinTour() {
        Tour tour = Tour.createDraft();
        tour.addPhoto(new Photo("photos/first.jpg", null, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> tour.addPhoto(new Photo("photos/second.jpg", null, 0))
        );
    }

    @Test
    void surfaceTypesCannotBeDuplicated() {
        Tour tour = Tour.createDraft();

        assertThrows(
                IllegalArgumentException.class,
                () -> tour.replaceSurfaceComposition(List.of(
                        new SurfaceShare(SurfaceType.GRAVEL, 40),
                        new SurfaceShare(SurfaceType.GRAVEL, 60)
                ))
        );
    }

    @Test
    void surfaceCompositionMustTotalOneHundredBeforePublication() {
        Tour tour = completeDraft(true);
        tour.replaceSurfaceComposition(List.of(
                new SurfaceShare(SurfaceType.ASPHALT, 30),
                new SurfaceShare(SurfaceType.SINGLETRACK, 60)
        ));

        assertThrows(
                IllegalStateException.class,
                () -> tour.publish(OffsetDateTime.parse("2026-08-22T12:00:00+02:00"))
        );
    }

    @Test
    void publicationRequiresRecommendedBikeType() {
        Tour tour = completeDraft(true);
        tour.replaceRecommendedBikeTypes(Set.of());

        assertThrows(
                IllegalStateException.class,
                () -> tour.publish(OffsetDateTime.parse("2026-08-22T12:00:00+02:00"))
        );
    }

    @Test
    void difficultyRatingsAreValidated() {
        Tour tour = Tour.createDraft();

        assertThrows(
                IllegalArgumentException.class,
                () -> tour.setDifficulty(TechnicalDifficulty.S3, 6, ExposureRating.E2, 4, 10)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> tour.setDifficulty(TechnicalDifficulty.S3, 4, ExposureRating.E2, 0, 10)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> tour.setDifficulty(TechnicalDifficulty.S3, 4, ExposureRating.E2, 4, 101)
        );
    }

    private static Tour completeDraft(boolean includeTourDate) {
        Tour tour = Tour.createDraft();
        tour.setIdentity("Mangart - Via Italiana", "Julian Alps", "IT");
        tour.setStartLocation(new StartLocation(
                "Passo Predil",
                new BigDecimal("46.4776000"),
                new BigDecimal("13.5321000")
        ));
        tour.setDescriptions(
                "Demanding alpine MTB tour.",
                "A long ascent followed by a technical descent."
        );
        tour.setStatistics(new TourStatistics(new BigDecimal("35.60"), 1650, 1650, 923, 2677));
        tour.setEstimatedDuration(new DurationRange(new BigDecimal("4.0"), new BigDecimal("5.0")));
        if (includeTourDate) {
            tour.setTourDate(LocalDate.of(2024, 5, 12));
        }
        tour.setDifficulty(TechnicalDifficulty.S4, 4, ExposureRating.E3, 5, 15);
        tour.setRouteType(RouteType.LOOP);
        tour.replaceRecommendedBikeTypes(Set.of(BikeType.ALL_MOUNTAIN));
        tour.replaceSurfaceComposition(List.of(
                new SurfaceShare(SurfaceType.ASPHALT, 15),
                new SurfaceShare(SurfaceType.GRAVEL, 30),
                new SurfaceShare(SurfaceType.FOREST_ROAD, 25),
                new SurfaceShare(SurfaceType.SINGLETRACK, 30)
        ));
        tour.setElevationProfile(List.of(
                new ElevationProfileSample(new BigDecimal("0.000"), 923),
                new ElevationProfileSample(new BigDecimal("35.600"), 923)
        ));
        tour.attachGpxTrack(new GpxTrack("mangart.gpx", "gpx/original/mangart.gpx"));
        return tour;
    }
}
