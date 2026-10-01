package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.dao.TrainingDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrainingServiceTest {

    @Mock
    private TrainingDAO trainingDAO;
    @Mock
    private TraineeDAO traineeDAO;
    @Mock
    private TrainerDAO trainerDAO;

    private TrainingService trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new TrainingService();
        trainingService.setTrainingDAO(trainingDAO);
        trainingService.setTraineeDAO(traineeDAO);
        trainingService.setTrainerDAO(trainerDAO);
    }

    @Test
    void testCreateTraining() {
        Trainee trainee = new Trainee();
        trainee.setPassword("pass");
        Trainer trainer = new Trainer();
        trainer.setSpecialization(TrainingType.YOGA);
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("trainer")).thenReturn(Optional.of(trainer));

        trainingService.createTraining("trainee", "pass", "trainer", "Training 1", LocalDate.now(), Duration.ofMinutes(60));

        verify(trainingDAO).save(any(Training.class));
    }

    @Test
    void testGetTraineeTrainingsAuthFailure() {
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.empty());
        assertThrows(SecurityException.class, () -> trainingService.getTraineeTrainings("trainee", "wrong", null, null, null, null));
    }

    @Test
    void testCreateTrainingValidation() {
        Trainee trainee = new Trainee();
        trainee.setPassword("pass");
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.of(trainee));
        
        assertThrows(IllegalArgumentException.class, () -> trainingService.createTraining("trainee", "pass", "trainer", "", LocalDate.now(), Duration.ZERO));
    }
}
