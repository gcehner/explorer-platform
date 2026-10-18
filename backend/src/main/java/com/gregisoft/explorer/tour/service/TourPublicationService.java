package com.gregisoft.explorer.tour.service;

import com.gregisoft.explorer.tour.api.TourDetailResponse;
import com.gregisoft.explorer.tour.entity.Tour;
import com.gregisoft.explorer.tour.repository.TourRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class TourPublicationService {

    private final TourRepository repository;

    public TourPublicationService(TourRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TourDetailResponse publish(long id) {
        Tour tour = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tour was not found."));

        try {
            OffsetDateTime publicationTime = OffsetDateTime.now(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            tour.publish(publicationTime);
        } catch (IllegalStateException exception) {
            throw TourUpdateException.unprocessable(exception.getMessage());
        }

        return TourReadService.toResponse(tour);
    }
}
