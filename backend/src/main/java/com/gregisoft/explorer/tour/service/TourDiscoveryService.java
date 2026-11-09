package com.gregisoft.explorer.tour.service;

import com.gregisoft.explorer.tour.api.TourPageResponse;
import com.gregisoft.explorer.tour.api.TourLatestResponse;
import com.gregisoft.explorer.tour.api.TourSummaryResponse;
import com.gregisoft.explorer.tour.repository.TourDiscoveryRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@ApplicationScoped
public class TourDiscoveryService {

    private static final int DEFAULT_PAGE_SIZE = 24;
    private static final int MAXIMUM_PAGE_SIZE = 50;
    private static final int DEFAULT_LATEST_LIMIT = 6;
    private static final int MAXIMUM_LATEST_LIMIT = 12;

    private final TourDiscoveryRepository repository;
    private final TourDiscoveryCursorCodec cursorCodec;

    public TourDiscoveryService(
            TourDiscoveryRepository repository,
            TourDiscoveryCursorCodec cursorCodec
    ) {
        this.repository = repository;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public TourPageResponse findTours(
            String bbox,
            String countryCode,
            String technicalDifficulty,
            String distanceMinKm,
            String distanceMaxKm,
            String ascentMinMeters,
            String ascentMaxMeters,
            String pageSize,
            String cursor
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
        int resolvedPageSize = parsePageSize(pageSize);
        String criteriaFingerprint = cursorCodec.fingerprint(criteria);

        OffsetDateTime snapshotAt;
        OffsetDateTime cursorPublishedAt = null;
        Long cursorId = null;
        if (cursor == null) {
            snapshotAt = OffsetDateTime.now(ZoneOffset.UTC);
        } else {
            TourDiscoveryCursorCodec.DecodedCursor decoded =
                    cursorCodec.decode(cursor, criteriaFingerprint);
            snapshotAt = decoded.snapshotAt();
            cursorPublishedAt = decoded.publishedAt();
            cursorId = decoded.id();
        }

        List<TourSummaryResponse> results = repository.findPublishedTours(
                criteria,
                snapshotAt,
                cursorPublishedAt,
                cursorId,
                resolvedPageSize + 1
        );
        boolean hasMore = results.size() > resolvedPageSize;
        List<TourSummaryResponse> items = hasMore
                ? results.subList(0, resolvedPageSize)
                : results;
        String nextCursor = null;
        if (hasMore) {
            TourSummaryResponse last = items.getLast();
            nextCursor = cursorCodec.encode(
                    snapshotAt,
                    last.publishedAt(),
                    last.id(),
                    criteriaFingerprint
            );
        }
        return new TourPageResponse(items, nextCursor, hasMore);
    }

    @Transactional
    public TourLatestResponse findLatestTours(String limit) {
        return new TourLatestResponse(
                repository.findLatestPublishedTours(parseLatestLimit(limit))
        );
    }

    private static int parseLatestLimit(String value) {
        if (value == null) {
            return DEFAULT_LATEST_LIMIT;
        }
        try {
            int limit = Integer.parseInt(value.trim());
            if (limit < 1 || limit > MAXIMUM_LATEST_LIMIT) {
                throw new TourDiscoveryException("limit must be between 1 and 12.");
            }
            return limit;
        } catch (NumberFormatException exception) {
            throw new TourDiscoveryException("limit must be an integer.");
        }
    }

    private static int parsePageSize(String value) {
        if (value == null) {
            return DEFAULT_PAGE_SIZE;
        }
        try {
            int pageSize = Integer.parseInt(value.trim());
            if (pageSize < 1 || pageSize > MAXIMUM_PAGE_SIZE) {
                throw new TourDiscoveryException("pageSize must be between 1 and 50.");
            }
            return pageSize;
        } catch (NumberFormatException exception) {
            throw new TourDiscoveryException("pageSize must be an integer.");
        }
    }
}
