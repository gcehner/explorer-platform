package com.gregisoft.explorer.tour.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gregisoft.explorer.tour.api.TourDetailResponse;
import com.gregisoft.explorer.tour.entity.Tour;
import com.gregisoft.explorer.tour.repository.TourRepository;
import com.gregisoft.explorer.tour.type.BikeType;
import com.gregisoft.explorer.tour.type.ExposureRating;
import com.gregisoft.explorer.tour.type.PublicationStatus;
import com.gregisoft.explorer.tour.type.RouteType;
import com.gregisoft.explorer.tour.type.SurfaceType;
import com.gregisoft.explorer.tour.type.TechnicalDifficulty;
import com.gregisoft.explorer.tour.value.DurationRange;
import com.gregisoft.explorer.tour.value.StartLocation;
import com.gregisoft.explorer.tour.value.SurfaceShare;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TourUpdateService {

    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "title",
            "mountainRange",
            "countryCode",
            "startLocation",
            "shortDescription",
            "technicalDescription",
            "estimatedDuration",
            "tourDate",
            "technicalDifficulty",
            "physicalDifficulty",
            "exposureRating",
            "scenicRating",
            "carryingPercentage",
            "routeType",
            "recommendedBikeTypes",
            "surfaceComposition",
            "youtubeVideoId"
    );

    private static final Set<String> START_LOCATION_FIELDS = Set.of(
            "name", "latitude", "longitude"
    );
    private static final Set<String> DURATION_FIELDS = Set.of(
            "minimumHours", "maximumHours"
    );
    private static final Set<String> SURFACE_SHARE_FIELDS = Set.of(
            "surfaceType", "percentage"
    );

    private final TourRepository repository;

    public TourUpdateService(TourRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TourDetailResponse updateDraft(long id, ObjectNode patch) {
        if (patch == null) {
            throw TourUpdateException.badRequest("A JSON object is required.");
        }
        rejectUnknownFields(patch, EDITABLE_FIELDS, "Tour patch");

        Tour tour = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tour was not found."));
        if (tour.getPublicationStatus() != PublicationStatus.DRAFT) {
            throw TourUpdateException.conflict("Only draft Tours can be edited.");
        }

        try {
            applyIdentity(patch, tour);
            applyStartLocation(patch, tour);
            applyDescriptions(patch, tour);
            applyEstimatedDuration(patch, tour);
            applyTourDate(patch, tour);
            applyDifficulty(patch, tour);
            applyRouteType(patch, tour);
            applyBikeTypes(patch, tour);
            applySurfaceComposition(patch, tour);
            applyYoutubeVideoId(patch, tour);
            return TourReadService.toResponse(tour);
        } catch (TourUpdateException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
            throw TourUpdateException.unprocessable(exception.getMessage());
        }
    }

    private static void applyIdentity(ObjectNode patch, Tour tour) {
        if (patch.has("title")) {
            tour.setTitle(text(patch, "title", 200));
        }
        if (patch.has("mountainRange")) {
            tour.setMountainRange(text(patch, "mountainRange", 200));
        }
        if (patch.has("countryCode")) {
            tour.setCountryCode(text(patch, "countryCode", 2));
        }
    }

    private static void applyStartLocation(ObjectNode patch, Tour tour) {
        if (!patch.has("startLocation")) {
            return;
        }
        ObjectNode value = object(patch, "startLocation");
        requireFields(value, START_LOCATION_FIELDS, "Start location");
        rejectUnknownFields(value, START_LOCATION_FIELDS, "Start location");
        tour.setStartLocation(new StartLocation(
                text(value, "name", 200),
                decimal(value, "latitude"),
                decimal(value, "longitude")
        ));
    }

    private static void applyDescriptions(ObjectNode patch, Tour tour) {
        if (patch.has("shortDescription")) {
            tour.setShortDescription(text(patch, "shortDescription", 500));
        }
        if (patch.has("technicalDescription")) {
            tour.setTechnicalDescription(text(patch, "technicalDescription", 10000));
        }
    }

    private static void applyEstimatedDuration(ObjectNode patch, Tour tour) {
        if (!patch.has("estimatedDuration")) {
            return;
        }
        ObjectNode value = object(patch, "estimatedDuration");
        requireFields(value, DURATION_FIELDS, "Estimated duration");
        rejectUnknownFields(value, DURATION_FIELDS, "Estimated duration");
        DurationRange duration = new DurationRange(
                decimal(value, "minimumHours"),
                decimal(value, "maximumHours")
        );
        if (duration.getMinimumHours().precision() > 4
                || duration.getMaximumHours().precision() > 4) {
            throw TourUpdateException.unprocessable(
                    "Estimated duration must fit NUMERIC(4,1)."
            );
        }
        tour.setEstimatedDuration(duration);
    }

    private static void applyTourDate(ObjectNode patch, Tour tour) {
        if (!patch.has("tourDate")) {
            return;
        }
        JsonNode value = requiredValue(patch, "tourDate");
        if (!value.isTextual()) {
            throw typeError("tourDate", "an ISO-8601 date string");
        }
        if (tour.getTourDate() != null) {
            throw TourUpdateException.unprocessable(
                    "Tour date cannot replace a date supplied by the GPX import."
            );
        }
        try {
            tour.setTourDate(LocalDate.parse(value.textValue()));
        } catch (DateTimeParseException exception) {
            throw TourUpdateException.badRequest("tourDate must be an ISO-8601 date string.");
        }
    }

    private static void applyDifficulty(ObjectNode patch, Tour tour) {
        if (patch.has("technicalDifficulty")) {
            tour.setTechnicalDifficulty(enumValue(
                    patch,
                    "technicalDifficulty",
                    TechnicalDifficulty.class
            ));
        }
        if (patch.has("physicalDifficulty")) {
            tour.setPhysicalDifficulty(integer(patch, "physicalDifficulty"));
        }
        if (patch.has("exposureRating")) {
            tour.setExposureRating(enumValue(patch, "exposureRating", ExposureRating.class));
        }
        if (patch.has("scenicRating")) {
            tour.setScenicRating(integer(patch, "scenicRating"));
        }
        if (patch.has("carryingPercentage")) {
            tour.setCarryingPercentage(integer(patch, "carryingPercentage"));
        }
    }

    private static void applyRouteType(ObjectNode patch, Tour tour) {
        if (patch.has("routeType")) {
            tour.setRouteType(enumValue(patch, "routeType", RouteType.class));
        }
    }

    private static void applyBikeTypes(ObjectNode patch, Tour tour) {
        if (!patch.has("recommendedBikeTypes")) {
            return;
        }
        ArrayNode values = array(patch, "recommendedBikeTypes");
        Set<BikeType> bikeTypes = EnumSet.noneOf(BikeType.class);
        for (JsonNode value : values) {
            BikeType bikeType = enumValue(value, "recommendedBikeTypes", BikeType.class);
            if (!bikeTypes.add(bikeType)) {
                throw TourUpdateException.unprocessable(
                        "Recommended bike types cannot contain duplicates."
                );
            }
        }
        tour.replaceRecommendedBikeTypes(bikeTypes);
    }

    private static void applySurfaceComposition(ObjectNode patch, Tour tour) {
        if (!patch.has("surfaceComposition")) {
            return;
        }
        ArrayNode values = array(patch, "surfaceComposition");
        List<SurfaceShare> shares = new ArrayList<>(values.size());
        Set<SurfaceType> types = new HashSet<>();
        for (JsonNode node : values) {
            if (!(node instanceof ObjectNode value)) {
                throw typeError("surfaceComposition", "an array of objects");
            }
            requireFields(value, SURFACE_SHARE_FIELDS, "Surface share");
            rejectUnknownFields(value, SURFACE_SHARE_FIELDS, "Surface share");
            SurfaceType surfaceType = enumValue(value, "surfaceType", SurfaceType.class);
            if (!types.add(surfaceType)) {
                throw TourUpdateException.unprocessable(
                        "Surface composition cannot contain duplicate surface types."
                );
            }
            shares.add(new SurfaceShare(
                    surfaceType,
                    integer(value, "percentage")
            ));
        }
        tour.replaceSurfaceComposition(shares);
    }

    private static void applyYoutubeVideoId(ObjectNode patch, Tour tour) {
        if (!patch.has("youtubeVideoId")) {
            return;
        }
        JsonNode value = patch.get("youtubeVideoId");
        if (value.isNull()) {
            tour.setYoutubeVideoId(null);
            return;
        }
        if (!value.isTextual()) {
            throw typeError("youtubeVideoId", "a string or null");
        }
        if (value.textValue().isBlank()) {
            throw TourUpdateException.unprocessable("youtubeVideoId must not be blank.");
        }
        tour.setYoutubeVideoId(value.textValue());
    }

    private static String text(ObjectNode object, String field, int maximumLength) {
        JsonNode value = requiredValue(object, field);
        if (!value.isTextual()) {
            throw typeError(field, "a string");
        }
        String text = value.textValue().trim();
        if (text.isEmpty()) {
            throw TourUpdateException.unprocessable(field + " must not be blank.");
        }
        if (text.length() > maximumLength) {
            throw TourUpdateException.unprocessable(
                    field + " must not exceed " + maximumLength + " characters."
            );
        }
        return text;
    }

    private static BigDecimal decimal(ObjectNode object, String field) {
        JsonNode value = requiredValue(object, field);
        if (!value.isNumber()) {
            throw typeError(field, "a number");
        }
        return value.decimalValue();
    }

    private static int integer(ObjectNode object, String field) {
        JsonNode value = requiredValue(object, field);
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw typeError(field, "an integer");
        }
        return value.intValue();
    }

    private static <E extends Enum<E>> E enumValue(
            ObjectNode object,
            String field,
            Class<E> enumType
    ) {
        return enumValue(requiredValue(object, field), field, enumType);
    }

    private static <E extends Enum<E>> E enumValue(
            JsonNode value,
            String field,
            Class<E> enumType
    ) {
        if (!value.isTextual()) {
            throw typeError(field, "a string");
        }
        try {
            return Enum.valueOf(enumType, value.textValue());
        } catch (IllegalArgumentException exception) {
            throw TourUpdateException.badRequest(
                    field + " contains an unknown " + enumType.getSimpleName() + " value."
            );
        }
    }

    private static ObjectNode object(ObjectNode patch, String field) {
        JsonNode value = requiredValue(patch, field);
        if (!(value instanceof ObjectNode object)) {
            throw typeError(field, "an object");
        }
        return object;
    }

    private static ArrayNode array(ObjectNode patch, String field) {
        JsonNode value = requiredValue(patch, field);
        if (!(value instanceof ArrayNode array)) {
            throw typeError(field, "an array");
        }
        return array;
    }

    private static JsonNode requiredValue(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            throw TourUpdateException.unprocessable(field + " must not be null.");
        }
        return value;
    }

    private static void requireFields(ObjectNode object, Set<String> fields, String context) {
        for (String field : fields) {
            if (!object.has(field)) {
                throw TourUpdateException.unprocessable(
                        context + " requires " + String.join(", ", fields) + "."
                );
            }
        }
    }

    private static void rejectUnknownFields(
            ObjectNode object,
            Set<String> allowedFields,
            String context
    ) {
        Iterator<String> names = object.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!allowedFields.contains(name)) {
                throw TourUpdateException.badRequest(
                        context + " contains unknown or read-only field '" + name + "'."
                );
            }
        }
    }

    private static TourUpdateException typeError(String field, String expectedType) {
        return TourUpdateException.badRequest(field + " must be " + expectedType + ".");
    }
}
