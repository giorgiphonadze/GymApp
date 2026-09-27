package com.gymcrm.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {

    @Test
    void testTrainee() {
        Trainee t1 = new Trainee("Giorgi", "Phonadze", LocalDate.now(), "Address");
        t1.setUserId(1L);
        t1.setUsername("Giorgi.Phonadze");
        t1.setPassword("pass");
        t1.setActive(true);

        assertEquals("Giorgi", t1.getFirstName());
        assertEquals("Phonadze", t1.getLastName());
        assertNotNull(t1.getDateOfBirth());
        assertEquals("Address", t1.getAddress());
        assertEquals(1L, t1.getUserId());
        assertEquals("Giorgi.Phonadze", t1.getUsername());
        assertEquals("pass", t1.getPassword());
        assertTrue(t1.isActive());

        Trainee t2 = new Trainee();
        t2.setUserId(1L);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotNull(t1.toString());
    }

    @Test
    void testTrainer() {
        TrainingType type = new TrainingType(1L, "Yoga");
        Trainer t1 = new Trainer("Irakli", "Kikvadze", type);
        t1.setUserId(2L);
        t1.setUsername("Irakli.Kikvadze");

        assertEquals("Irakli", t1.getFirstName());
        assertEquals("Kikvadze", t1.getLastName());
        assertEquals(type, t1.getSpecialization());
        assertEquals(2L, t1.getUserId());

        Trainer t2 = new Trainer();
        t2.setUserId(2L);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotNull(t1.toString());
    }

    @Test
    void testTraining() {
        TrainingType type = new TrainingType("Fitness");
        Training tr1 = new Training(1L, 2L, "Workout", type, LocalDate.now(), 60);
        tr1.setId(10L);

        assertEquals(1L, tr1.getTrainerId());
        assertEquals(2L, tr1.getTraineeId());
        assertEquals("Workout", tr1.getTrainingName());
        assertEquals(type, tr1.getTrainingType());
        assertNotNull(tr1.getTrainingDate());
        assertEquals(60, tr1.getTrainingDuration());
        assertEquals(10L, tr1.getId());

        Training tr2 = new Training(1L, 2L, "Workout", type, LocalDate.now(), 60);
        tr2.setId(10L);
        assertEquals(tr1, tr2);
        assertEquals(tr1.hashCode(), tr2.hashCode());
        assertNotNull(tr1.toString());
        
        assertNotEquals(tr1, new Object());
        assertNotEquals(tr1, null);
    }

    @Test
    void testTrainingType() {
        TrainingType tt1 = new TrainingType(1L, "Yoga");
        TrainingType tt2 = new TrainingType("Yoga");
        
        assertEquals("Yoga", tt1.getTrainingTypeName());
        assertEquals(1L, tt1.getId());
        assertEquals(tt1, tt2);
        assertEquals(tt1.hashCode(), tt2.hashCode());
        assertNotNull(tt1.toString());
        
        tt2.setTrainingTypeName("Pilates");
        assertNotEquals(tt1, tt2);
    }
    
    @Test
    void testUserEquals() {
        Trainee t1 = new Trainee("A", "B", null, null);
        t1.setUserId(1L);
        Trainer tr1 = new Trainer("C", "D", null);
        tr1.setUserId(1L);
        
        // Even if IDs are same, if they are different classes, are they equal?
        // User.equals uses instanceof User.
        assertEquals(t1, tr1); // Currently it just checks userId
        
        t1.setUserId(null);
        assertNotEquals(t1, tr1);
        
        assertNotEquals(t1, new Object());
    }
}
