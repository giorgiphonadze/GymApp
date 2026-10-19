package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.dao.TrainingDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingDAO trainingDAO;
    @Mock
    private TraineeDAO traineeDAO;
    @Mock
    private TrainerDAO trainerDAO;

    private SimpleMeterRegistry meterRegistry;
    private TrainingService trainingService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        GymMetrics metrics = new GymMetrics(meterRegistry);
        trainingService = new TrainingService(trainingDAO, trainerDAO,
                new AuthenticationService(traineeDAO, trainerDAO, metrics), metrics);
    }

    private Trainee traineeWithPassword(String password) {
        Trainee trainee = new Trainee();
        trainee.setPassword(password);
        return trainee;
    }

    private Trainer trainerWithPassword(String password) {
        Trainer trainer = new Trainer();
        trainer.setPassword(password);
        trainer.setSpecialization(TrainingType.YOGA);
        return trainer;
    }

    @Test
    void createTrainingUsesTrainerSpecializationAndRecordsMetric() {
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.of(traineeWithPassword("pass")));
        when(trainerDAO.findByUsername("trainer")).thenReturn(Optional.of(trainerWithPassword("secret")));

        trainingService.createTraining("trainee", "trainer", "Training 1", LocalDate.now(), Duration.ofMinutes(60));

        ArgumentCaptor<Training> captor = ArgumentCaptor.forClass(Training.class);
        verify(trainingDAO).save(captor.capture());
        assertEquals(TrainingType.YOGA, captor.getValue().getTrainingType());
        assertEquals("Training 1", captor.getValue().getTrainingName());
        assertEquals(1.0, meterRegistry.get("gym.trainings.created").tag("trainingType", "yoga").counter().count());
    }

    @Test
    void createTrainingFailsWhenTrainerIsUnknown() {
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.of(traineeWithPassword("pass")));
        when(trainerDAO.findByUsername("trainer")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> trainingService
                .createTraining("trainee", "trainer", "Training 1", LocalDate.now(), Duration.ofMinutes(60)));

        verify(trainingDAO, never()).save(any());
    }

    @Test
    void createTrainingValidatesInput() {
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.of(traineeWithPassword("pass")));
        when(trainerDAO.findByUsername("trainer")).thenReturn(Optional.of(trainerWithPassword("secret")));

        assertThrows(IllegalArgumentException.class, () -> trainingService
                .createTraining("trainee", "trainer", "", LocalDate.now(), Duration.ZERO));

        verify(trainingDAO, never()).save(any());
    }

    @Test
    void getTraineeTrainingsRequiresExistingTrainee() {
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.empty());

        assertThrows(PasswordException.class,
                () -> trainingService.getTraineeTrainings("trainee", null, null, null, null));
    }

    @Test
    void getTraineeTrainingsDelegatesFiltersToDao() {
        List<Training> expected = List.of(new Training());
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.of(traineeWithPassword("pass")));
        when(trainingDAO.findTraineeTrainings("trainee", null, null, "Alice", "YOGA")).thenReturn(expected);

        assertSame(expected, trainingService.getTraineeTrainings("trainee", null, null, "Alice", "YOGA"));
    }

    @Test
    void getTrainerTrainingsRequiresExistingTrainer() {
        when(trainerDAO.findByUsername("trainer")).thenReturn(Optional.empty());

        assertThrows(PasswordException.class,
                () -> trainingService.getTrainerTrainings("trainer", null, null, null));
    }

    @Test
    void getTrainerTrainingsDelegatesFiltersToDao() {
        List<Training> expected = List.of(new Training());
        when(trainerDAO.findByUsername("trainer")).thenReturn(Optional.of(trainerWithPassword("pass")));
        when(trainingDAO.findTrainerTrainings("trainer", null, null, "Bob")).thenReturn(expected);

        assertSame(expected, trainingService.getTrainerTrainings("trainer", null, null, "Bob"));
    }

    @Test
    void getTrainingTypesReturnsAllTypes() {
        assertEquals(List.of(TrainingType.values()), trainingService.getTrainingTypes());
    }
}
