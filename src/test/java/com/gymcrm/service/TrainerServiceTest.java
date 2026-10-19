package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
class TrainerServiceTest {

    @Mock
    private TrainerDAO trainerDAO;
    @Mock
    private TraineeDAO traineeDAO;

    private SimpleMeterRegistry meterRegistry;
    private TrainerService trainerService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        GymMetrics metrics = new GymMetrics(meterRegistry);
        trainerService = new TrainerService(trainerDAO, traineeDAO,
                new AuthenticationService(traineeDAO, trainerDAO, metrics), metrics);
    }

    private Trainer trainerWithPassword(String password) {
        Trainer trainer = new Trainer("Alice", "Jones", TrainingType.YOGA);
        trainer.setPassword(password);
        return trainer;
    }

    @Test
    void createTrainerGeneratesCredentialsAndRecordsRegistration() {
        Trainer trainer = new Trainer("Alice", "Jones", TrainingType.YOGA);
        when(trainerDAO.findAll()).thenReturn(Collections.emptyList());
        when(traineeDAO.findAll()).thenReturn(Collections.emptyList());
        when(trainerDAO.save(any(Trainer.class))).thenAnswer(i -> i.getArgument(0));

        Trainer created = trainerService.createTrainer(trainer);

        assertEquals("Alice.Jones", created.getUsername());
        assertNotNull(created.getPassword());
        verify(trainerDAO).save(trainer);
        assertEquals(1.0, meterRegistry.get("gym.registrations").tag("userType", "trainer").counter().count());
    }

    @Test
    void createTrainerWithoutSpecializationIsRejected() {
        Trainer trainer = new Trainer("Alice", "Jones", null);

        assertThrows(IllegalArgumentException.class, () -> trainerService.createTrainer(trainer));

        verify(trainerDAO, never()).save(any());
    }

    @Test
    void updateTrainerRequiresExistingTrainer() {
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.empty());

        assertThrows(PasswordException.class,
                () -> trainerService.updateTrainer("Alice.Jones", "Alicia", "Jones", true));
    }

    @Test
    void updateTrainerRejectsMissingNames() {
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.of(trainerWithPassword("pass")));

        assertThrows(IllegalArgumentException.class,
                () -> trainerService.updateTrainer("Alice.Jones", "", "Jones", true));

        verify(trainerDAO, never()).update(any());
    }

    @Test
    void updateTrainerAppliesChangesToExistingTrainerAndKeepsSpecialization() {
        Trainer existing = trainerWithPassword("pass");
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.of(existing));
        when(trainerDAO.update(existing)).thenReturn(existing);

        Trainer updated = trainerService.updateTrainer("Alice.Jones", "Alicia", "Jones", false);

        assertSame(existing, updated);
        assertEquals("Alicia", updated.getFirstName());
        assertFalse(updated.isActive());
        assertEquals(TrainingType.YOGA, updated.getSpecialization());
    }

    @Test
    void getTrainerByUsernameReturnsAuthenticatedTrainer() {
        Trainer trainer = trainerWithPassword("pass");
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.of(trainer));

        assertSame(trainer, trainerService.getTrainerByUsername("Alice.Jones").orElseThrow());
    }

    @Test
    void authenticateChecksPassword() {
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.of(trainerWithPassword("pass")));

        assertTrue(trainerService.authenticate("Alice.Jones", "pass"));
        assertFalse(trainerService.authenticate("Alice.Jones", "wrong"));
    }

    @Test
    void changePasswordStoresNewPassword() {
        Trainer trainer = trainerWithPassword("old");
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.of(trainer));

        trainerService.changePassword("Alice.Jones", "old", "new");

        assertEquals("new", trainer.getPassword());
        verify(trainerDAO).update(trainer);
    }

    @Test
    void activateDeactivateChangesState() {
        Trainer trainer = trainerWithPassword("pass");
        trainer.setActive(true);
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.of(trainer));

        trainerService.activateDeactivate("Alice.Jones", false);

        assertFalse(trainer.isActive());
        verify(trainerDAO).update(trainer);
    }

    @Test
    void activateDeactivateRejectsNoopChange() {
        Trainer trainer = trainerWithPassword("pass");
        trainer.setActive(true);
        when(trainerDAO.findByUsername("Alice.Jones")).thenReturn(Optional.of(trainer));

        assertThrows(IllegalStateException.class, () -> trainerService.activateDeactivate("Alice.Jones", true));
    }

    @Test
    void getTrainersNotAssignedToTraineeLoadsExistingTrainee() {
        Trainee trainee = new Trainee();
        trainee.setPassword("pass");
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.of(trainee));

        trainerService.getTrainersNotAssignedToTrainee("trainee");

        verify(trainerDAO).findNotAssignedToTrainee("trainee");
    }

    @Test
    void getTrainersNotAssignedToTraineeRejectsUnknownTrainee() {
        when(traineeDAO.findByUsername("trainee")).thenReturn(Optional.empty());

        assertThrows(PasswordException.class, () -> trainerService.getTrainersNotAssignedToTrainee("trainee"));

        verify(trainerDAO, never()).findNotAssignedToTrainee(any());
    }
}
