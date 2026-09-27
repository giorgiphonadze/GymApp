package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrainerServiceTest {

    @Mock
    private TrainerDAO trainerDAO;
    @Mock
    private TraineeDAO traineeDAO;

    private TrainerService trainerService;

    @BeforeEach
    void setUp() {
        trainerService = new TrainerService();
        trainerService.setTrainerDAO(trainerDAO);
        trainerService.setTraineeDAO(traineeDAO);
    }

    @Test
    void testCreateTrainer() {
        Trainer trainer = new Trainer("Alice", "Jones", new TrainingType("Yoga"));
        when(trainerDAO.findAll()).thenReturn(Collections.emptyList());
        when(traineeDAO.findAll()).thenReturn(Collections.emptyList());
        when(trainerDAO.save(any(Trainer.class))).thenAnswer(i -> i.getArgument(0));

        Trainer created = trainerService.createTrainer(trainer);

        assertEquals("Alice.Jones", created.getUsername());
        assertNotNull(created.getPassword());
        verify(trainerDAO).save(trainer);
    }

    @Test
    void testGetTrainer() {
        Trainer trainer = new Trainer();
        when(trainerDAO.findById(1L)).thenReturn(Optional.of(trainer));

        Optional<Trainer> found = trainerService.getTrainer(1L);

        assertTrue(found.isPresent());
        verify(trainerDAO).findById(1L);
    }
}
