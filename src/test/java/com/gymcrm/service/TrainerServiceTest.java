package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainee;
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
        Trainer trainer = new Trainer("Alice", "Jones", TrainingType.YOGA);
        when(trainerDAO.findAll()).thenReturn(Collections.emptyList());
        when(traineeDAO.findAll()).thenReturn(Collections.emptyList());
        when(trainerDAO.save(any(Trainer.class))).thenAnswer(i -> i.getArgument(0));

        Trainer created = trainerService.createTrainer(trainer);

        assertEquals("Alice.Jones", created.getUsername());
        assertNotNull(created.getPassword());
        verify(trainerDAO).save(trainer);
    }

    @Test
    void testUpdateTrainerAuthFailure() {
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.empty());
        assertThrows(SecurityException.class, () -> trainerService.updateTrainer("Alice.Jones", "wrong", new Trainer()));
    }

    @Test
    void testChangePassword() {
        Trainer trainer = new Trainer();
        trainer.setPassword("old");
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.of(trainer));
        
        trainerService.changePassword("Alice.Jones", "old", "new");
        assertEquals("new", trainer.getPassword());
        verify(trainerDAO).update(trainer);
    }

    @Test
    void testActivateDeactivate() {
        Trainer trainer = new Trainer();
        trainer.setPassword("pass");
        trainer.setActive(true);
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.of(trainer));
        
        trainerService.activateDeactivate("Alice.Jones", "pass", false);
        assertFalse(trainer.isActive());
        verify(trainerDAO).update(trainer);
    }

    @Test
    void testGetTrainersNotAssignedToTrainee() {
        Trainee trainee = new Trainee();
        trainee.setPassword("pass");
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.of(trainee));
        
        trainerService.getTrainersNotAssignedToTrainee("trainee", "pass");
        verify(trainerDAO).findNotAssignedToTrainee("trainee");
    }
}
