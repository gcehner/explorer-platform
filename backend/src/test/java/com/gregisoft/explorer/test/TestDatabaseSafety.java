package com.gregisoft.explorer.test;

import jakarta.persistence.EntityManager;

public final class TestDatabaseSafety {

    private static final String EXPECTED_DATABASE = "explorer_test";

    private TestDatabaseSafety() {
    }

    public static void assertUsingIsolatedTestDatabase(EntityManager entityManager) {
        String actualDatabase = (String) entityManager
                .createNativeQuery("select current_database()")
                .getSingleResult();

        if (!EXPECTED_DATABASE.equals(actualDatabase)) {
            throw new IllegalStateException(
                    "Refusing to reset test fixtures in database '" + actualDatabase
                            + "'; expected '" + EXPECTED_DATABASE + "'."
            );
        }
    }
}
