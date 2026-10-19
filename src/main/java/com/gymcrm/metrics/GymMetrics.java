package com.gymcrm.metrics;

import com.gymcrm.domain.TrainingType;
import com.gymcrm.domain.UserType;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class GymMetrics {
    static final String REGISTRATIONS = "gym.registrations";
    static final String AUTHENTICATIONS = "gym.authentications";
    static final String TRAININGS_CREATED = "gym.trainings.created";

    private static final Logger log = LoggerFactory.getLogger(GymMetrics.class);

    private final MeterRegistry registry;

    public GymMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRegistration(UserType userType) {
        registry.counter(REGISTRATIONS, "userType", tag(userType)).increment();
        log.debug("Recorded registration metric for {}", userType);
    }

    public void recordAuthentication(UserType userType, boolean success) {
        registry.counter(AUTHENTICATIONS, "userType", tag(userType), "result", success ? "success" : "failure")
                .increment();
    }

    public void recordTrainingCreated(TrainingType trainingType) {
        registry.counter(TRAININGS_CREATED, "trainingType", tag(trainingType)).increment();
        log.debug("Recorded training created metric for {}", trainingType);
    }

    private static String tag(Enum<?> value) {
        return value == null ? "unknown" : value.name().toLowerCase(Locale.ROOT);
    }
}
