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

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Entity
@Table(name = "tour")
public class Tour {

    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");
    private static final Pattern YOUTUBE_VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "mountain_range", length = 200)
    private String mountainRange;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Embedded
    private StartLocation startLocation;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "technical_description", length = 10000)
    private String technicalDescription;

    @Embedded
    private TourStatistics statistics;

    @Embedded
    private DurationRange estimatedDuration;

    @Column(name = "tour_date")
    private LocalDate tourDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "technical_difficulty", length = 2)
    private TechnicalDifficulty technicalDifficulty;

    @Column(name = "physical_difficulty")
    private Integer physicalDifficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "exposure_rating", length = 2)
    private ExposureRating exposureRating;

    @Column(name = "scenic_rating")
    private Integer scenicRating;

    @Column(name = "carrying_percentage")
    private Integer carryingPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", length = 30)
    private RouteType routeType;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tour_bike_type",
            joinColumns = @JoinColumn(name = "tour_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"tour_id", "bike_type"})
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "bike_type", nullable = false, length = 30)
    private Set<BikeType> recommendedBikeTypes = EnumSet.noneOf(BikeType.class);

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tour_surface_share",
            joinColumns = @JoinColumn(name = "tour_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"tour_id", "surface_type"})
    )
    @OrderColumn(name = "surface_order")
    private List<SurfaceShare> surfaceComposition = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tour_elevation_profile", joinColumns = @JoinColumn(name = "tour_id"))
    @OrderColumn(name = "sample_index")
    private List<ElevationProfileSample> elevationProfile = new ArrayList<>();

    @OneToOne(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private GpxTrack gpxTrack;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<Photo> photos = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_photo_id")
    private Photo coverPhoto;

    @Column(name = "youtube_video_id", length = 20)
    private String youtubeVideoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected Tour() {
        // Required by Jakarta Persistence.
    }

    public static Tour createDraft() {
        return new Tour();
    }

    public void setIdentity(String title, String mountainRange, String countryCode) {
        String normalizedTitle = requireText(title, "Title");
        String normalizedMountainRange = requireText(mountainRange, "Mountain range");
        String normalizedCountryCode = requireText(countryCode, "Country code")
                .toUpperCase(Locale.ROOT);
        if (!COUNTRY_CODE.matcher(normalizedCountryCode).matches()) {
            throw new IllegalArgumentException("Country code must be an ISO 3166-1 alpha-2 code.");
        }

        this.title = normalizedTitle;
        this.mountainRange = normalizedMountainRange;
        this.countryCode = normalizedCountryCode;
    }

    public void setStartLocation(StartLocation startLocation) {
        this.startLocation = Objects.requireNonNull(startLocation, "Start location is required.");
    }

    public void setDescriptions(String shortDescription, String technicalDescription) {
        String normalizedShortDescription = requireText(shortDescription, "Short description");
        String normalizedTechnicalDescription = requireText(
                technicalDescription,
                "Technical description"
        );
        this.shortDescription = normalizedShortDescription;
        this.technicalDescription = normalizedTechnicalDescription;
    }

    public void setStatistics(TourStatistics statistics) {
        this.statistics = Objects.requireNonNull(statistics, "Tour statistics are required.");
    }

    public void setEstimatedDuration(DurationRange estimatedDuration) {
        this.estimatedDuration = Objects.requireNonNull(
                estimatedDuration,
                "Estimated duration is required."
        );
    }

    public void setTourDate(LocalDate tourDate) {
        this.tourDate = Objects.requireNonNull(tourDate, "Tour date is required.");
    }

    public void setDifficulty(
            TechnicalDifficulty technicalDifficulty,
            int physicalDifficulty,
            ExposureRating exposureRating,
            int scenicRating,
            int carryingPercentage
    ) {
        TechnicalDifficulty newTechnicalDifficulty = Objects.requireNonNull(
                technicalDifficulty,
                "Technical difficulty is required."
        );
        ExposureRating newExposureRating = Objects.requireNonNull(
                exposureRating,
                "Exposure rating is required."
        );
        int newPhysicalDifficulty = requireRating(physicalDifficulty, "Physical difficulty");
        int newScenicRating = requireRating(scenicRating, "Scenic rating");
        if (carryingPercentage < 0 || carryingPercentage > 100) {
            throw new IllegalArgumentException("Carrying percentage must be between 0 and 100.");
        }

        this.technicalDifficulty = newTechnicalDifficulty;
        this.exposureRating = newExposureRating;
        this.physicalDifficulty = newPhysicalDifficulty;
        this.scenicRating = newScenicRating;
        this.carryingPercentage = carryingPercentage;
    }

    public void setRouteType(RouteType routeType) {
        this.routeType = Objects.requireNonNull(routeType, "Route type is required.");
    }

    public void replaceRecommendedBikeTypes(Set<BikeType> bikeTypes) {
        Objects.requireNonNull(bikeTypes, "Recommended bike types are required.");
        if (bikeTypes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Recommended bike types must not contain null.");
        }
        if (publicationStatus == PublicationStatus.PUBLISHED && bikeTypes.isEmpty()) {
            throw new IllegalStateException("A published Tour must recommend at least one bike type.");
        }
        recommendedBikeTypes.clear();
        recommendedBikeTypes.addAll(bikeTypes);
    }

    public void replaceSurfaceComposition(List<SurfaceShare> surfaceShares) {
        Objects.requireNonNull(surfaceShares, "Surface composition is required.");
        validateSurfaceTypesAreUnique(surfaceShares);
        if (publicationStatus == PublicationStatus.PUBLISHED) {
            validateSurfaceTotal(surfaceShares);
        }
        surfaceComposition.clear();
        surfaceComposition.addAll(surfaceShares);
    }

    public void setElevationProfile(List<ElevationProfileSample> samples) {
        Objects.requireNonNull(samples, "Elevation profile is required.");
        validateElevationProfile(samples);
        if (publicationStatus == PublicationStatus.PUBLISHED && samples.isEmpty()) {
            throw new IllegalStateException("A published Tour must have an elevation profile.");
        }
        elevationProfile.clear();
        elevationProfile.addAll(samples);
    }

    public void attachGpxTrack(GpxTrack track) {
        Objects.requireNonNull(track, "GPX track is required.");
        if (gpxTrack != null && gpxTrack != track) {
            throw new IllegalStateException("V1 does not support replacing a Tour's GPX track.");
        }
        track.attachTo(this);
        gpxTrack = track;
    }

    public void addPhoto(Photo photo) {
        Objects.requireNonNull(photo, "Photo is required.");
        ensureDisplayOrderAvailable(photo.getDisplayOrder(), null);
        photo.attachTo(this);
        photos.add(photo);
    }

    public void reorderPhoto(Photo photo, int displayOrder) {
        requireOwnedPhoto(photo);
        ensureDisplayOrderAvailable(displayOrder, photo);
        photo.setDisplayOrder(displayOrder);
    }

    public void removePhoto(Photo photo) {
        requireOwnedPhoto(photo);
        if (coverPhoto == photo) {
            coverPhoto = null;
        }
        photos.remove(photo);
        photo.detachFrom(this);
    }

    public void selectCoverPhoto(Photo photo) {
        requireOwnedPhoto(photo);
        coverPhoto = photo;
    }

    public void clearCoverPhoto() {
        coverPhoto = null;
    }

    public void setYoutubeVideoId(String youtubeVideoId) {
        String normalizedVideoId = normalize(youtubeVideoId);
        if (normalizedVideoId != null && !YOUTUBE_VIDEO_ID.matcher(normalizedVideoId).matches()) {
            throw new IllegalArgumentException("YouTube video ID must contain 11 valid characters.");
        }
        this.youtubeVideoId = normalizedVideoId;
    }

    public void publish(OffsetDateTime publicationTime) {
        if (publicationStatus == PublicationStatus.PUBLISHED) {
            return;
        }
        validateForPublication();
        publishedAt = Objects.requireNonNull(publicationTime, "Publication time is required.");
        publicationStatus = PublicationStatus.PUBLISHED;
    }

    private void validateForPublication() {
        requirePresent(title, "Title");
        requirePresent(mountainRange, "Mountain range");
        requirePresent(countryCode, "Country code");
        requirePresent(startLocation, "Start location");
        requirePresent(shortDescription, "Short description");
        requirePresent(technicalDescription, "Technical description");
        requirePresent(statistics, "Tour statistics");
        requirePresent(estimatedDuration, "Estimated duration");
        requirePresent(tourDate, "Tour date");
        requirePresent(technicalDifficulty, "Technical difficulty");
        requirePresent(physicalDifficulty, "Physical difficulty");
        requirePresent(exposureRating, "Exposure rating");
        requirePresent(scenicRating, "Scenic rating");
        requirePresent(carryingPercentage, "Carrying percentage");
        requirePresent(routeType, "Route type");
        requirePresent(gpxTrack, "GPX track");

        if (recommendedBikeTypes.isEmpty()) {
            throw new IllegalStateException("A Tour must recommend at least one bike type before publication.");
        }
        validateSurfaceTotal(surfaceComposition);
        if (elevationProfile.isEmpty()) {
            throw new IllegalStateException("A Tour must have an elevation profile before publication.");
        }
        if (coverPhoto != null && !photos.contains(coverPhoto)) {
            throw new IllegalStateException("The cover photo must belong to the Tour.");
        }
    }

    private static void validateSurfaceTypesAreUnique(List<SurfaceShare> shares) {
        Set<SurfaceType> surfaceTypes = new HashSet<>();
        for (SurfaceShare share : shares) {
            if (share == null) {
                throw new IllegalArgumentException("Surface composition must not contain null.");
            }
            if (!surfaceTypes.add(share.getSurfaceType())) {
                throw new IllegalArgumentException(
                        "Surface composition cannot contain duplicate surface types."
                );
            }
        }
    }

    private static void validateSurfaceTotal(List<SurfaceShare> shares) {
        int total = shares.stream().mapToInt(SurfaceShare::getPercentage).sum();
        if (total != 100) {
            throw new IllegalStateException("Surface composition must total 100 before publication.");
        }
    }

    private static void validateElevationProfile(List<ElevationProfileSample> samples) {
        ElevationProfileSample previous = null;
        for (ElevationProfileSample sample : samples) {
            if (sample == null) {
                throw new IllegalArgumentException("Elevation profile must not contain null samples.");
            }
            if (previous != null
                    && sample.getDistanceKm().compareTo(previous.getDistanceKm()) < 0) {
                throw new IllegalArgumentException(
                        "Elevation profile samples must be ordered by distance."
                );
            }
            previous = sample;
        }
    }

    private void requireOwnedPhoto(Photo photo) {
        if (photo == null || photo.getTour() != this || !photos.contains(photo)) {
            throw new IllegalArgumentException("Photo must belong to this Tour.");
        }
    }

    private void ensureDisplayOrderAvailable(int displayOrder, Photo photoToIgnore) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Photo display order must be zero or greater.");
        }
        boolean duplicate = photos.stream()
                .anyMatch(photo -> photo != photoToIgnore && photo.getDisplayOrder() == displayOrder);
        if (duplicate) {
            throw new IllegalArgumentException(
                    "Photo display order must be unique within a Tour."
            );
        }
    }

    private static int requireRating(int rating, String fieldName) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 5.");
        }
        return rating;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void requirePresent(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(fieldName + " is required before publication.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMountainRange() {
        return mountainRange;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public StartLocation getStartLocation() {
        return startLocation;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getTechnicalDescription() {
        return technicalDescription;
    }

    public TourStatistics getStatistics() {
        return statistics;
    }

    public DurationRange getEstimatedDuration() {
        return estimatedDuration;
    }

    public LocalDate getTourDate() {
        return tourDate;
    }

    public TechnicalDifficulty getTechnicalDifficulty() {
        return technicalDifficulty;
    }

    public Integer getPhysicalDifficulty() {
        return physicalDifficulty;
    }

    public ExposureRating getExposureRating() {
        return exposureRating;
    }

    public Integer getScenicRating() {
        return scenicRating;
    }

    public Integer getCarryingPercentage() {
        return carryingPercentage;
    }

    public RouteType getRouteType() {
        return routeType;
    }

    public Set<BikeType> getRecommendedBikeTypes() {
        return Collections.unmodifiableSet(recommendedBikeTypes);
    }

    public List<SurfaceShare> getSurfaceComposition() {
        return List.copyOf(surfaceComposition);
    }

    public List<ElevationProfileSample> getElevationProfile() {
        return List.copyOf(elevationProfile);
    }

    public GpxTrack getGpxTrack() {
        return gpxTrack;
    }

    public List<Photo> getPhotos() {
        return List.copyOf(photos);
    }

    public Photo getCoverPhoto() {
        return coverPhoto;
    }

    public String getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public PublicationStatus getPublicationStatus() {
        return publicationStatus;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}
