package com.gymcrm.service;

import com.gymcrm.dao.TrainingDAO;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrainingServiceTest {

    @Mock
    private TrainingDAO trainingDAO;

    private TrainingService trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new TrainingService();
        trainingService.setTrainingDAO(trainingDAO);
    }

    @Test
    void testCreateTraining() {
        Training training = new Training(1L, 2L, "Training 1", new TrainingType("Yoga"), LocalDate.now(), 60);
        when(trainingDAO.save(any(Training.class))).thenAnswer(i -> i.getArgument(0));

        Training created = trainingService.createTraining(training);

        assertNotNull(created);
        assertEquals("Training 1", created.getTrainingName());
        verify(trainingDAO).save(training);
    }

    @Test
    void testGetTraining() {
        Training training = new Training(1L, 2L, "Training 1", new TrainingType("Yoga"), LocalDate.now(), 60);
        when(trainingDAO.findById(1L)).thenReturn(Optional.of(training));

        Optional<Training> found = trainingService.getTraining(1L);

        assertTrue(found.isPresent());
        verify(trainingDAO).findById(1L);
    }
}
