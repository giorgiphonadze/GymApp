package com.gymcrm.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymDatabaseHealthIndicatorTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private DatabaseMetaData metaData;

    private GymDatabaseHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new GymDatabaseHealthIndicator(dataSource);
    }

    @Test
    void isUpWhenConnectionIsValid() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(GymDatabaseHealthIndicator.VALIDATION_TIMEOUT_SECONDS)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("H2", health.getDetails().get("database"));
        verify(connection).close();
    }

    @Test
    void isDownWhenConnectionIsNotValid() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(GymDatabaseHealthIndicator.VALIDATION_TIMEOUT_SECONDS)).thenReturn(false);

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("reason"));
    }

    @Test
    void isDownWhenConnectionCannotBeObtained() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("db unreachable"));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }
}
