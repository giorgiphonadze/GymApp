package com.gymcrm.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class GymDatabaseHealthIndicator extends AbstractHealthIndicator {
    static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private static final Logger log = LoggerFactory.getLogger(GymDatabaseHealthIndicator.class);

    private final DataSource dataSource;

    public GymDatabaseHealthIndicator(DataSource dataSource) {
        super("Gym database health check failed");
        this.dataSource = dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                builder.up().withDetail("database", connection.getMetaData().getDatabaseProductName());
                log.debug("Gym database is reachable");
            } else {
                log.warn("Gym database connection is not valid");
                builder.down().withDetail("reason", "Connection is not valid");
            }
        }
    }
}
