package com.gymcrm.metrics;

import com.gymcrm.domain.TrainingType;
import com.gymcrm.domain.UserType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GymMetricsTest {

    private SimpleMeterRegistry registry;
    private GymMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new GymMetrics(registry);
    }

    @Test
    void registrationsAreCountedPerUserType() {
        metrics.recordRegistration(UserType.TRAINEE);
        metrics.recordRegistration(UserType.TRAINEE);
        metrics.recordRegistration(UserType.TRAINER);

        assertEquals(2.0, registry.get(GymMetrics.REGISTRATIONS).tag("userType", "trainee").counter().count());
        assertEquals(1.0, registry.get(GymMetrics.REGISTRATIONS).tag("userType", "trainer").counter().count());
    }

    @Test
    void authenticationsAreCountedPerResult() {
        metrics.recordAuthentication(UserType.TRAINEE, true);
        metrics.recordAuthentication(UserType.TRAINEE, false);
        metrics.recordAuthentication(UserType.TRAINEE, false);

        assertEquals(1.0, registry.get(GymMetrics.AUTHENTICATIONS)
                .tag("userType", "trainee").tag("result", "success").counter().count());
        assertEquals(2.0, registry.get(GymMetrics.AUTHENTICATIONS)
                .tag("userType", "trainee").tag("result", "failure").counter().count());
    }

    @Test
    void createdTrainingsAreCountedPerTrainingType() {
        metrics.recordTrainingCreated(TrainingType.BJJ);
        metrics.recordTrainingCreated(null);

        assertEquals(1.0, registry.get(GymMetrics.TRAININGS_CREATED).tag("trainingType", "bjj").counter().count());
        assertEquals(1.0, registry.get(GymMetrics.TRAININGS_CREATED).tag("trainingType", "unknown").counter().count());
    }
}
