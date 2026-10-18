package com.gregisoft.explorer.test;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.eclipse.microprofile.config.ConfigProvider;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class TestDatabaseConfigurationGuard implements QuarkusTestBeforeEachCallback {

    private static final String EXPECTED_DATABASE = "explorer_test";

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        String jdbcUrl = ConfigProvider.getConfig()
                .getValue("quarkus.datasource.jdbc.url", String.class);

        String database = URI.create(jdbcUrl.substring("jdbc:".length())).getPath();
        if (!("/" + EXPECTED_DATABASE).equals(database)) {
            throw new IllegalStateException(
                    "Refusing to run @QuarkusTest against JDBC URL '" + jdbcUrl
                            + "'; expected database '" + EXPECTED_DATABASE + "'."
            );
        }

        AgroalDataSource dataSource = Arc.container().instance(AgroalDataSource.class).get();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select current_database()")) {
            result.next();
            String actualDatabase = result.getString(1);
            if (!EXPECTED_DATABASE.equals(actualDatabase)) {
                throw new IllegalStateException(
                        "Refusing to run @QuarkusTest against database '" + actualDatabase
                                + "'; expected '" + EXPECTED_DATABASE + "'."
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Unable to verify the isolated integration-test database.",
                    exception
            );
        }
    }
}
