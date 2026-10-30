package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.UserType;
import com.gymcrm.metrics.GymMetrics;
import com.gymcrm.utils.UserUtils;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Service
public class TrainerService {
    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private final TrainerDAO trainerDAO;
    private final TraineeDAO traineeDAO;
    private final AuthenticationService authenticationService;
    private final GymMetrics metrics;

    public TrainerService(TrainerDAO trainerDAO, TraineeDAO traineeDAO,
                          AuthenticationService authenticationService, GymMetrics metrics) {
        this.trainerDAO = trainerDAO;
        this.traineeDAO = traineeDAO;
        this.authenticationService = authenticationService;
        this.metrics = metrics;
    }

    @Transactional
    public Trainer createTrainer(Trainer trainer) {
        log.debug("Creating trainer {} {}", trainer.getFirstName(), trainer.getLastName());
        validateTrainer(trainer);
        Set<String> existingUsernames = UserUtils.collectUsernames(traineeDAO.findAll(), trainerDAO.findAll());
        trainer.setUsername(UserUtils.generateUsername(trainer.getFirstName(), trainer.getLastName(), existingUsernames));
        trainer.setPassword(UserUtils.generatePassword());
        trainer.setActive(true);
        Trainer saved = trainerDAO.save(trainer);
        metrics.recordRegistration(UserType.TRAINER);
        log.info("Created Trainer: {}", saved.getUsername());
        return saved;
    }

    @Transactional
    public Trainer updateTrainer(String username, String firstName, String lastName, boolean isActive) {
        Trainer trainer = authenticationService.requireTrainer(username);
        if (firstName == null || firstName.isEmpty() || lastName == null || lastName.isEmpty()) {
            log.warn("Rejected trainer update without first or last name");
            throw new IllegalArgumentException("First name and Last name are required");
        }
        trainer.setFirstName(firstName);
        trainer.setLastName(lastName);
        trainer.setActive(isActive);
        Trainer updated = trainerDAO.update(trainer);
        Hibernate.initialize(updated.getTrainees());
        log.info("Updated Trainer: {}", username);
        return updated;
    }

    @Transactional(readOnly = true)
    public Optional<Trainer> getTrainerByUsername(String username) {
        Trainer trainer = authenticationService.requireTrainer(username);
        Hibernate.initialize(trainer.getTrainees());
        return Optional.of(trainer);
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String username, String password) {
        return authenticationService.isValidTrainer(username, password);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Trainer trainer = authenticationService.authenticateTrainer(username, oldPassword);
        trainer.setPassword(newPassword);
        trainerDAO.update(trainer);
        log.info("Changed password for Trainer: {}", username);
    }

    @Transactional
    public void activateDeactivate(String username, boolean isActive) {
        Trainer trainer = authenticationService.requireTrainer(username);
        if (trainer.isActive() == isActive) {
            log.warn("Trainer {} is already {}", username, isActive ? "active" : "inactive");
            throw new IllegalStateException("Trainer is already " + (isActive ? "active" : "inactive"));
        }
        trainer.setActive(isActive);
        trainerDAO.update(trainer);
        log.info("{} Trainer: {}", isActive ? "Activated" : "De-activated", username);
    }

    @Transactional(readOnly = true)
    public Collection<Trainer> getTrainersNotAssignedToTrainee(String traineeUsername) {
        authenticationService.requireTrainee(traineeUsername);
        return trainerDAO.findNotAssignedToTrainee(traineeUsername);
    }

    private void validateTrainer(Trainer trainer) {
        if (trainer.getFirstName() == null || trainer.getFirstName().isEmpty() ||
                trainer.getLastName() == null || trainer.getLastName().isEmpty() ||
                trainer.getSpecialization() == null) {
            log.warn("Rejected trainer with missing first name, last name or specialization");
            throw new IllegalArgumentException("First name, Last name and Specialization are required");
        }
    }
}
