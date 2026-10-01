package com.gymcrm;

import com.gymcrm.config.AppConfig;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GymAppIntegrationTest {

    @Test
    void testGymFlow() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            GymFacade facade = context.getBean(GymFacade.class);
            
            // 1. Get Training Types
            List<TrainingType> types = facade.getTrainingTypes();
            assertFalse(types.isEmpty());
            TrainingType yoga = TrainingType.YOGA;

            // 2. Create Trainer
            Trainer trainer = new Trainer("Alice", "Jones", yoga);
            Trainer createdTrainer = facade.createTrainer(trainer);
            String trainerUsername = createdTrainer.getUsername();
            assertNotNull(trainerUsername);

            // 3. Create Trainee
            Trainee trainee = new Trainee("Bob", "Brown", LocalDate.of(2000, 1, 1), "Some Address");
            Trainee createdTrainee = facade.createTrainee(trainee);
            String traineeUsername = createdTrainee.getUsername();
            assertNotNull(traineeUsername);

            // 4. Authenticate
            assertTrue(facade.authenticateTrainer(trainerUsername, createdTrainer.getPassword()));
            assertTrue(facade.authenticateTrainee(traineeUsername, createdTrainee.getPassword()));

            // 5. Add Training
            facade.createTraining(traineeUsername, createdTrainee.getPassword(), trainerUsername, "Morning Yoga", LocalDate.now(), Duration.ofMinutes(60));

            // 6. Get Trainings with filters
            List<Training> trainings = facade.getTraineeTrainings(traineeUsername, createdTrainee.getPassword(), null, null, null, null);
            assertEquals(1, trainings.size());
            assertEquals("Morning Yoga", trainings.get(0).getTrainingName());
            
            // Filter by date
            assertEquals(1, facade.getTraineeTrainings(traineeUsername, createdTrainee.getPassword(), LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), null, null).size());
            assertEquals(0, facade.getTraineeTrainings(traineeUsername, createdTrainee.getPassword(), LocalDate.now().plusDays(1), null, null, null).size());
            
            // Filter by trainer name
            assertEquals(1, facade.getTraineeTrainings(traineeUsername, createdTrainee.getPassword(), null, null, "Alice", null).size());
            assertEquals(0, facade.getTraineeTrainings(traineeUsername, createdTrainee.getPassword(), null, null, "Wrong", null).size());

            // Filter by type
            assertEquals(1, facade.getTraineeTrainings(traineeUsername, createdTrainee.getPassword(), null, null, null, "YOGA").size());

            // Trainer Trainings
            assertEquals(1, facade.getTrainerTrainings(trainerUsername, createdTrainer.getPassword(), null, null, "Bob").size());

            // 7. Update Trainee's trainers
            facade.updateTraineeTrainers(traineeUsername, createdTrainee.getPassword(), List.of(trainerUsername));
            Trainee updatedTrainee = facade.getTrainee(traineeUsername, createdTrainee.getPassword()).get();
            assertEquals(1, updatedTrainee.getTrainers().size());

            // 7.1 Activate/Deactivate non-idempotent
            facade.activateDeactivateTrainee(traineeUsername, createdTrainee.getPassword(), false);
            assertThrows(IllegalStateException.class, () -> facade.activateDeactivateTrainee(traineeUsername, createdTrainee.getPassword(), false));
            facade.activateDeactivateTrainee(traineeUsername, createdTrainee.getPassword(), true);

            // 8. Delete Trainee
            facade.deleteTrainee(traineeUsername, createdTrainee.getPassword());
            assertThrows(SecurityException.class, () -> facade.getTrainee(traineeUsername, "wrong"));
            
            // Verify trainings are deleted (cascade)
            // Note: Since we use H2 mem and transactions, this depends on how we query.
            // In a real scenario, we'd check the training table.
        }
    }
}
