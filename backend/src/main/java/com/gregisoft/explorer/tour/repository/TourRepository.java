package com.gregisoft.explorer.tour.repository;

import com.gregisoft.explorer.tour.entity.Tour;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class TourRepository {

    private final EntityManager entityManager;

    public TourRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void persistAndFlush(Tour tour) {
        entityManager.persist(tour);
        entityManager.flush();
    }
}
