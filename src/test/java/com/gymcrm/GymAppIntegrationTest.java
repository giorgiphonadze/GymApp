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
            
            // 1. Get Training Types (enum-based)
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
            facade.createTraining(traineeUsername, trainerUsername, "Morning Yoga", LocalDate.now(), Duration.ofMinutes(60));

            // 6. Get Trainings
            List<Training> trainings = facade.getTraineeTrainings(traineeUsername, null, null, null, null);
            assertEquals(1, trainings.size());
            assertEquals("Morning Yoga", trainings.get(0).getTrainingName());

            // 7. Update Trainee's trainers
            facade.updateTraineeTrainers(traineeUsername, List.of(trainerUsername));
            Trainee updatedTrainee = facade.getTrainee(traineeUsername).get();
            assertEquals(1, updatedTrainee.getTrainers().size());

            // 8. Delete Trainee
            facade.deleteTrainee(traineeUsername);
            assertFalse(facade.getTrainee(traineeUsername).isPresent());
            
            // Verify trainings are deleted (cascade)
            // Note: Since we use H2 mem and transactions, this depends on how we query.
            // In a real scenario, we'd check the training table.
        }
    }
}
