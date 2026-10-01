package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainee;
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
public class TraineeServiceTest {

    @Mock
    private TraineeDAO traineeDAO;
    @Mock
    private TrainerDAO trainerDAO;

    private TraineeService traineeService;

    @BeforeEach
    void setUp() {
        traineeService = new TraineeService();
        traineeService.setTraineeDAO(traineeDAO);
        traineeService.setTrainerDAO(trainerDAO);
    }

    @Test
    void testCreateTrainee() {
        Trainee trainee = new Trainee("John", "Smith", null, "Addr");
        when(traineeDAO.findAll()).thenReturn(Collections.emptyList());
        when(trainerDAO.findAll()).thenReturn(Collections.emptyList());
        when(traineeDAO.save(any(Trainee.class))).thenAnswer(i -> i.getArgument(0));

        Trainee created = traineeService.createTrainee(trainee);

        assertEquals("John.Smith", created.getUsername());
        assertNotNull(created.getPassword());
        verify(traineeDAO).save(trainee);
    }

    @Test
    void testGetTraineeByUsername() {
        Trainee trainee = new Trainee();
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        Optional<Trainee> found = traineeService.getTraineeByUsername("John.Smith");

        assertTrue(found.isPresent());
        verify(traineeDAO).findByUsername("John.Smith");
    }

    @Test
    void testDeleteTrainee() {
        Trainee trainee = new Trainee();
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        traineeService.deleteTrainee("John.Smith");
        verify(traineeDAO).delete(trainee);
    }
}
