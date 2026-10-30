package com.gymcrm.health;

import com.gymcrm.service.TrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class TrainingCatalogHealthIndicator extends AbstractHealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(TrainingCatalogHealthIndicator.class);

    private final TrainingService trainingService;

    public TrainingCatalogHealthIndicator(TrainingService trainingService) {
        super("Training catalog health check failed");
        this.trainingService = trainingService;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        int trainingTypes = trainingService.getTrainingTypes().size();
        if (trainingTypes > 0) {
            builder.up().withDetail("trainingTypes", trainingTypes);
        } else {
            log.warn("No training types are available");
            builder.down().withDetail("reason", "No training types available");
        }
    }
}
