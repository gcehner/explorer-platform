package com.gregisoft.explorer.info.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.util.Optional;

import com.gregisoft.explorer.info.entity.ApplicationInfo;

@ApplicationScoped
public class ApplicationInfoRepository {

    private static final int APPLICATION_INFO_ID = 1;

    private final EntityManager entityManager;

    public ApplicationInfoRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<ApplicationInfo> findApplicationInfo() {
        ApplicationInfo applicationInfo =
                entityManager.find(ApplicationInfo.class, APPLICATION_INFO_ID);

        return Optional.ofNullable(applicationInfo);
    }
}
