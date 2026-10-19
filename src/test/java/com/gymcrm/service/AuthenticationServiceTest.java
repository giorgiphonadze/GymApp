package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.UserType;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private TraineeDAO traineeDAO;
    @Mock
    private TrainerDAO trainerDAO;

    private SimpleMeterRegistry registry;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new AuthenticationService(traineeDAO, trainerDAO, new GymMetrics(registry));
    }

    private static Trainee trainee(String password) {
        Trainee trainee = new Trainee();
        trainee.setPassword(password);
        return trainee;
    }

    private static Trainer trainer(String password) {
        Trainer trainer = new Trainer();
        trainer.setPassword(password);
        return trainer;
    }

    @Test
    void authenticateTraineeReturnsTraineeWhenPasswordMatches() {
        Trainee trainee = trainee("pass");
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.of(trainee));

        assertSame(trainee, service.authenticateTrainee("t", "pass"));
        assertEquals(1.0, registry.get("gym.authentications")
                .tag("userType", "trainee").tag("result", "success").counter().count());
    }

    @Test
    void authenticateTraineeFailsOnWrongPassword() {
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.of(trainee("pass")));

        assertThrows(PasswordException.class, () -> service.authenticateTrainee("t", "wrong"));
        assertEquals(1.0, registry.get("gym.authentications")
                .tag("userType", "trainee").tag("result", "failure").counter().count());
    }

    @Test
    void authenticateTraineeFailsOnUnknownUser() {
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.empty());

        assertThrows(PasswordException.class, () -> service.authenticateTrainee("t", "pass"));
    }

    @Test
    void authenticateTrainerReturnsTrainerWhenPasswordMatches() {
        Trainer trainer = trainer("pass");
        when(trainerDAO.findByUsername("tr")).thenReturn(Optional.of(trainer));

        assertSame(trainer, service.authenticateTrainer("tr", "pass"));
    }

    @Test
    void authenticateTrainerFailsOnWrongPassword() {
        when(trainerDAO.findByUsername("tr")).thenReturn(Optional.of(trainer("pass")));

        assertThrows(PasswordException.class, () -> service.authenticateTrainer("tr", "wrong"));
    }

    @Test
    void validityChecksDoNotThrow() {
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.of(trainee("pass")));
        when(trainerDAO.findByUsername("tr")).thenReturn(Optional.of(trainer("pass")));

        assertTrue(service.isValidTrainee("t", "pass"));
        assertFalse(service.isValidTrainee("t", "wrong"));
        assertTrue(service.isValidTrainer("tr", "pass"));
        assertFalse(service.isValidTrainer("tr", "wrong"));
    }

    @Test
    void loginAcceptsTraineeCredentials() {
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.of(trainee("pass")));

        assertTrue(service.login("t", "pass"));
        assertEquals(1.0, registry.get("gym.authentications")
                .tag("userType", "trainee").tag("result", "success").counter().count());
    }

    @Test
    void loginAcceptsTrainerCredentialsWithoutCountingTraineeFailure() {
        when(traineeDAO.findByUsername("tr")).thenReturn(Optional.empty());
        when(trainerDAO.findByUsername("tr")).thenReturn(Optional.of(trainer("pass")));

        assertTrue(service.login("tr", "pass"));
        assertEquals(1.0, registry.get("gym.authentications")
                .tag("userType", "trainer").tag("result", "success").counter().count());
        assertNull(registry.find("gym.authentications").tag("result", "failure").counter());
    }

    @Test
    void loginRejectsWrongPasswordOfTrainee() {
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.of(trainee("pass")));

        assertFalse(service.login("t", "wrong"));
    }

    @Test
    void loginRejectsUnknownUser() {
        when(traineeDAO.findByUsername("nobody")).thenReturn(Optional.empty());
        when(trainerDAO.findByUsername("nobody")).thenReturn(Optional.empty());

        assertFalse(service.login("nobody", "pass"));
    }

    @Test
    void identifyReportsTheTypeOfTheAuthenticatedUser() {
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.of(trainee("pass")));
        when(traineeDAO.findByUsername("tr")).thenReturn(Optional.empty());
        when(trainerDAO.findByUsername("tr")).thenReturn(Optional.of(trainer("pass")));

        assertEquals(Optional.of(UserType.TRAINEE), service.identify("t", "pass"));
        assertEquals(Optional.of(UserType.TRAINER), service.identify("tr", "pass"));
        assertEquals(Optional.empty(), service.identify("t", "wrong"));
    }

    @Test
    void requireTraineeAndTrainerLoadExistingUsersWithoutPassword() {
        Trainee trainee = trainee("pass");
        Trainer trainer = trainer("pass");
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.of(trainee));
        when(trainerDAO.findByUsername("tr")).thenReturn(Optional.of(trainer));

        assertSame(trainee, service.requireTrainee("t"));
        assertSame(trainer, service.requireTrainer("tr"));
    }

    @Test
    void requireTraineeAndTrainerRejectDeletedUsers() {
        when(traineeDAO.findByUsername("gone")).thenReturn(Optional.empty());
        when(trainerDAO.findByUsername("gone")).thenReturn(Optional.empty());

        assertThrows(PasswordException.class, () -> service.requireTrainee("gone"));
        assertThrows(PasswordException.class, () -> service.requireTrainer("gone"));
    }

    @Test
    void nullPasswordNeverMatches() {
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.of(trainee("pass")));

        assertFalse(service.isValidTrainee("t", null));
    }

    @Test
    void storedNullPasswordNeverMatches() {
        when(traineeDAO.findByUsername("t")).thenReturn(Optional.of(trainee(null)));

        assertFalse(service.isValidTrainee("t", "anything"));
    }
}
