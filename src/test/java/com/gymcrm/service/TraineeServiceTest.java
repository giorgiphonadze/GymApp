package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock
    private TraineeDAO traineeDAO;
    @Mock
    private TrainerDAO trainerDAO;

    private SimpleMeterRegistry meterRegistry;
    private TraineeService traineeService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        GymMetrics metrics = new GymMetrics(meterRegistry);
        traineeService = new TraineeService(traineeDAO, trainerDAO,
                new AuthenticationService(traineeDAO, trainerDAO, metrics), metrics);
    }

    private Trainee traineeWithPassword(String password) {
        Trainee trainee = new Trainee("John", "Smith", null, "Addr");
        trainee.setPassword(password);
        return trainee;
    }

    @Test
    void createTraineeGeneratesCredentialsAndRecordsRegistration() {
        Trainee trainee = new Trainee("John", "Smith", null, "Addr");
        when(traineeDAO.findAll()).thenReturn(Collections.emptyList());
        when(trainerDAO.findAll()).thenReturn(Collections.emptyList());
        when(traineeDAO.save(any(Trainee.class))).thenAnswer(i -> i.getArgument(0));

        Trainee created = traineeService.createTrainee(trainee);

        assertEquals("John.Smith", created.getUsername());
        assertEquals(10, created.getPassword().length());
        assertTrue(created.isActive());
        verify(traineeDAO).save(trainee);
        assertEquals(1.0, meterRegistry.get("gym.registrations").tag("userType", "trainee").counter().count());
    }

    @Test
    void createTraineeWithoutNameIsRejected() {
        Trainee trainee = new Trainee("", "Smith", null, "Addr");

        assertThrows(IllegalArgumentException.class, () -> traineeService.createTrainee(trainee));

        verify(traineeDAO, never()).save(any());
    }

    @Test
    void getTraineeByUsernameReturnsExistingTrainee() {
        Trainee trainee = traineeWithPassword("pass");
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        Optional<Trainee> found = traineeService.getTraineeByUsername("John.Smith");

        assertTrue(found.isPresent());
        assertSame(trainee, found.get());
    }

    @Test
    void getTraineeByUsernameRejectsUnknownUser() {
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertThrows(PasswordException.class, () -> traineeService.getTraineeByUsername("John.Smith"));
    }

    @Test
    void authenticateReturnsFalseForUnknownUser() {
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertFalse(traineeService.authenticate("John.Smith", "pass"));
    }

    @Test
    void authenticateReturnsTrueForMatchingPassword() {
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(traineeWithPassword("pass")));

        assertTrue(traineeService.authenticate("John.Smith", "pass"));
    }

    @Test
    void deleteTraineeRequiresExistingTrainee() {
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertThrows(PasswordException.class, () -> traineeService.deleteTrainee("John.Smith"));

        verify(traineeDAO, never()).delete(any());
    }

    @Test
    void deleteTraineeRemovesExistingTrainee() {
        Trainee trainee = traineeWithPassword("pass");
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        traineeService.deleteTrainee("John.Smith");

        verify(traineeDAO).delete(trainee);
    }

    @Test
    void activateDeactivateIsNotIdempotent() {
        Trainee trainee = traineeWithPassword("pass");
        trainee.setActive(true);
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        assertThrows(IllegalStateException.class, () -> traineeService.activateDeactivate("John.Smith", true));
        traineeService.activateDeactivate("John.Smith", false);

        assertFalse(trainee.isActive());
        verify(traineeDAO).update(trainee);
    }

    @Test
    void updateTraineeValidatesProfile() {
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(traineeWithPassword("pass")));

        assertThrows(IllegalArgumentException.class,
                () -> traineeService.updateTrainee("John.Smith", null, "", null, null, true));

        verify(traineeDAO, never()).update(any());
    }

    @Test
    void updateTraineeAppliesChangesToExistingTraineeAndKeepsItsRelations() {
        Trainee existing = traineeWithPassword("pass");
        Set<Trainer> trainers = Set.of(new Trainer());
        existing.setTrainers(trainers);
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(existing));
        when(traineeDAO.update(existing)).thenReturn(existing);

        Trainee updated = traineeService.updateTrainee("John.Smith", "Johnny", "Smith",
                LocalDate.of(1990, 5, 1), "New address", false);

        assertSame(existing, updated);
        assertEquals("Johnny", updated.getFirstName());
        assertEquals("New address", updated.getAddress());
        assertEquals(LocalDate.of(1990, 5, 1), updated.getDateOfBirth());
        assertFalse(updated.isActive());
        assertEquals("pass", updated.getPassword());
        assertSame(trainers, updated.getTrainers());
    }

    @Test
    void changePasswordStoresNewPassword() {
        Trainee trainee = traineeWithPassword("old");
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));

        traineeService.changePassword("John.Smith", "old", "new");

        assertEquals("new", trainee.getPassword());
        verify(traineeDAO).update(trainee);
    }

    @Test
    void updateTrainersListIgnoresUnknownTrainers() {
        Trainee trainee = traineeWithPassword("pass");
        Trainer known = new Trainer();
        when(traineeDAO.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("known")).thenReturn(Optional.of(known));
        when(trainerDAO.findByUsername("unknown")).thenReturn(Optional.empty());

        traineeService.updateTrainersList("John.Smith", List.of("known", "unknown"));

        assertEquals(1, trainee.getTrainers().size());
        assertTrue(trainee.getTrainers().contains(known));
        verify(traineeDAO).update(trainee);
    }
}
