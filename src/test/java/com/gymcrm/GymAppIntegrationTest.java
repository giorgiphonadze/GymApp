package com.gymcrm;

import com.gymcrm.config.AppConfig;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.storage.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class GymAppIntegrationTest {

    @Test
    void testInitialDataLoading() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            Storage storage = context.getBean(Storage.class);

            Collection<Object> trainees = storage.findAll("Trainee");
            assertEquals(2, trainees.size(), "Should have loaded 2 trainees from file");

            Collection<Object> trainers = storage.findAll("Trainer");
            assertEquals(1, trainers.size(), "Should have loaded 1 trainer from file");

            boolean foundOriginal = false;
            boolean foundSuffix = false;
            for (Object obj : trainees) {
                Trainee t = (Trainee) obj;
                if ("John.Smith".equals(t.getUsername())) foundOriginal = true;
                if ("John.Smith1".equals(t.getUsername())) foundSuffix = true;
            }
            assertTrue(foundOriginal, "John.Smith username should exist");
            assertTrue(foundSuffix, "John.Smith1 username should exist");
        }
    }

    @Test
    void testFacadeInjections() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            GymFacade facade = context.getBean(GymFacade.class);
            assertNotNull(facade);
            
            Trainee newTrainee = new Trainee("Bob", "Brown", null, "New St");
            Trainee created = facade.createTrainee(newTrainee);
            assertNotNull(created.getUsername());
            assertEquals("Bob.Brown", created.getUsername());
            assertNotNull(created.getPassword());
            assertEquals(10, created.getPassword().length());
            
            // Test update
            created.setAddress("Updated St");
            facade.updateTrainee(created);
            assertEquals("Updated St", facade.getTrainee(created.getUserId()).get().getAddress());
            
            // Test delete
            facade.deleteTrainee(created.getUserId());
            assertFalse(facade.getTrainee(created.getUserId()).isPresent());
        }
    }

    @Test
    void testTrainerAndTrainingFacade() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            GymFacade facade = context.getBean(GymFacade.class);
            
            Trainer trainer = new Trainer("Miles", "Davis", new TrainingType("Yoga"));
            Trainer createdTrainer = facade.createTrainer(trainer);
            assertNotNull(createdTrainer.getUsername());
            assertEquals("Miles.Davis", createdTrainer.getUsername());
            
            createdTrainer.setFirstName("Miles");
            facade.updateTrainer(createdTrainer);
            assertEquals("Miles", facade.getTrainer(createdTrainer.getUserId()).get().getFirstName());
            
            Trainee trainee = new Trainee("David", "Evans", null, null);
            facade.createTrainee(trainee);
            
            Training training = new Training(createdTrainer.getUserId(), trainee.getUserId(), "Morning Yoga", new TrainingType("Yoga"), LocalDate.now(), 45);
            Training createdTraining = facade.createTraining(training);
            assertNotNull(createdTraining.getId());
            
            assertTrue(facade.getTraining(createdTraining.getId()).isPresent());
        }
    }
}
