package com.gregisoft.explorer.tour.repository;

import com.gregisoft.explorer.tour.entity.Tour;
import com.gregisoft.explorer.tour.type.ImportSourceProvider;
import com.gregisoft.explorer.tour.type.PublicationStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.util.Optional;

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

    public Optional<Tour> findById(long id) {
        return Optional.ofNullable(entityManager.find(Tour.class, id));
    }

    public Optional<Tour> findPublishedById(long id) {
        return entityManager.createQuery(
                        "select tour from Tour tour "
                                + "where tour.id = :id "
                                + "and tour.publicationStatus = :status",
                        Tour.class
                )
                .setParameter("id", id)
                .setParameter("status", PublicationStatus.PUBLISHED)
                .getResultStream()
                .findFirst();
    }

    public Optional<Tour> findByImportSource(
            ImportSourceProvider sourceProvider,
            String sourceActivityId
    ) {
        return entityManager.createQuery(
                        "select track.tour from GpxTrack track "
                                + "where track.sourceProvider = :sourceProvider "
                                + "and track.sourceActivityId = :sourceActivityId",
                        Tour.class
                )
                .setParameter("sourceProvider", sourceProvider)
                .setParameter("sourceActivityId", sourceActivityId)
                .getResultStream()
                .findFirst();
    }

    public void lockImportSource(ImportSourceProvider sourceProvider, String sourceActivityId) {
        String lockIdentity = sourceProvider.name() + ":" + sourceActivityId;
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(:identity as text), 0))"
                )
                .setParameter("identity", lockIdentity)
                .getSingleResult();
    }
}
