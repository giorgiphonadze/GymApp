package com.gymcrm.health;

import com.gymcrm.domain.TrainingType;
import com.gymcrm.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingCatalogHealthIndicatorTest {

    @Mock
    private TrainingService trainingService;

    private TrainingCatalogHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new TrainingCatalogHealthIndicator(trainingService);
    }

    @Test
    void isUpAndReportsNumberOfTrainingTypes() {
        when(trainingService.getTrainingTypes()).thenReturn(List.of(TrainingType.YOGA, TrainingType.BJJ));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(2, health.getDetails().get("trainingTypes"));
    }

    @Test
    void isDownWhenCatalogIsEmpty() {
        when(trainingService.getTrainingTypes()).thenReturn(List.of());

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void isDownWhenCatalogCannotBeRead() {
        when(trainingService.getTrainingTypes()).thenThrow(new IllegalStateException("boom"));

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }
}
