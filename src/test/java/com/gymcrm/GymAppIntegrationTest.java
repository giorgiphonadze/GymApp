package com.gymcrm;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.domain.UserType;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.facade.GymFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class GymAppIntegrationTest {

    @Autowired
    private GymFacade facade;

    @Test
    void completeGymFlow() {
        Trainer createdTrainer = facade.createTrainer(new Trainer("Alice", "Jones", TrainingType.YOGA));
        String trainerUsername = createdTrainer.getUsername();
        assertNotNull(trainerUsername);

        Trainee createdTrainee = facade.createTrainee(new Trainee("Bob", "Brown", LocalDate.of(2000, 1, 1), "Some Address"));
        String traineeUsername = createdTrainee.getUsername();
        String traineePassword = createdTrainee.getPassword();

        assertTrue(facade.authenticateTrainer(trainerUsername, createdTrainer.getPassword()));
        assertTrue(facade.authenticateTrainee(traineeUsername, traineePassword));
        assertTrue(facade.authenticate(traineeUsername, traineePassword));
        assertFalse(facade.authenticate(traineeUsername, "wrong"));
        assertEquals(Optional.of(UserType.TRAINEE), facade.login(traineeUsername, traineePassword));
        assertEquals(Optional.of(UserType.TRAINER), facade.login(trainerUsername, createdTrainer.getPassword()));
        assertEquals(Optional.empty(), facade.login(traineeUsername, "wrong"));

        facade.createTraining(traineeUsername, trainerUsername, "Morning Yoga",
                LocalDate.now(), Duration.ofMinutes(60));

        List<Training> trainings = facade.getTraineeTrainings(traineeUsername, null, null, null, null);
        assertEquals(1, trainings.size());
        assertEquals("Morning Yoga", trainings.get(0).getTrainingName());

        assertEquals(1, facade.getTraineeTrainings(traineeUsername,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), null, null).size());
        assertEquals(0, facade.getTraineeTrainings(traineeUsername,
                LocalDate.now().plusDays(1), null, null, null).size());
        assertEquals(1, facade.getTraineeTrainings(traineeUsername, null, null, "Alice", null).size());
        assertEquals(0, facade.getTraineeTrainings(traineeUsername, null, null, "Wrong", null).size());
        assertEquals(1, facade.getTraineeTrainings(traineeUsername, null, null, null, "YOGA").size());
        assertEquals(1, facade.getTrainerTrainings(trainerUsername, null, null, "Bob").size());

        assertEquals(1, facade.getTrainersNotAssignedToTrainee(traineeUsername).stream()
                .filter(trainer -> trainer.getUsername().equals(trainerUsername)).count());
        facade.updateTraineeTrainers(traineeUsername, List.of(trainerUsername));
        assertEquals(1, facade.getTrainee(traineeUsername).orElseThrow().getTrainers().size());
        assertEquals(0, facade.getTrainersNotAssignedToTrainee(traineeUsername).stream()
                .filter(trainer -> trainer.getUsername().equals(trainerUsername)).count());

        facade.activateDeactivateTrainee(traineeUsername, false);
        assertThrows(IllegalStateException.class,
                () -> facade.activateDeactivateTrainee(traineeUsername, false));
        facade.activateDeactivateTrainee(traineeUsername, true);

        facade.changeTraineePassword(traineeUsername, traineePassword, "newPassword1");
        assertTrue(facade.authenticateTrainee(traineeUsername, "newPassword1"));
        assertFalse(facade.authenticateTrainee(traineeUsername, traineePassword));

        facade.deleteTrainee(traineeUsername);
        assertThrows(PasswordException.class, () -> facade.getTrainee(traineeUsername));
    }

    @Test
    void trainerLifecycle() {
        Trainer trainer = facade.createTrainer(new Trainer("Carl", "Coach", TrainingType.BOXING));
        String username = trainer.getUsername();
        String password = trainer.getPassword();

        assertEquals(trainer.getUserId(), facade.getTrainer(username).orElseThrow().getUserId());

        facade.changeTrainerPassword(username, password, "newPassword2");
        assertFalse(facade.authenticateTrainer(username, password));
        assertTrue(facade.authenticateTrainer(username, "newPassword2"));

        facade.activateDeactivateTrainer(username, false);
        assertFalse(facade.getTrainer(username).orElseThrow().isActive());

        Trainer updated = facade.updateTrainer(username, "Carlos", "Coach", true);
        assertEquals("Carlos", updated.getFirstName());
        assertTrue(updated.isActive());
        assertEquals("Carlos", facade.getTrainer(username).orElseThrow().getFirstName());
        assertEquals(TrainingType.BOXING, facade.getTrainer(username).orElseThrow().getSpecialization());
    }

    @Test
    void traineeProfileCanBeUpdated() {
        Trainee trainee = facade.createTrainee(new Trainee("Dina", "Dancer", null, "Old address"));
        String username = trainee.getUsername();

        facade.updateTrainee(username, "Dina", "Dancer", null, "New address", true);

        assertEquals("New address", facade.getTrainee(username).orElseThrow().getAddress());
        assertEquals(TrainingType.values().length, facade.getTrainingTypes().size());
    }
}
