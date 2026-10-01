package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainer;
import com.gymcrm.utils.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Service
public class TrainerService {
    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerDAO trainerDAO;
    private TraineeDAO traineeDAO;

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
    }

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) {
        this.traineeDAO = traineeDAO;
    }

    @Transactional
    public Trainer createTrainer(Trainer trainer) {
        validateTrainer(trainer);
        Set<String> existingUsernames = UserUtils.collectUsernames(traineeDAO.findAll(), trainerDAO.findAll());
        trainer.setUsername(UserUtils.generateUsername(trainer.getFirstName(), trainer.getLastName(), existingUsernames));
        trainer.setPassword(UserUtils.generatePassword());
        trainer.setActive(true);
        Trainer saved = trainerDAO.save(trainer);
        log.info("Created Trainer: {}", saved.getUsername());
        return saved;
    }

    @Transactional
    public void updateTrainer(String username, String password, Trainer trainer) {
        if (!authenticate(username, password)) {
            throw new SecurityException("Authentication failed");
        }
        validateTrainer(trainer);
        trainerDAO.update(trainer);
        log.info("Updated Trainer: {}", trainer.getUsername());
    }

    @Transactional(readOnly = true)
    public Optional<Trainer> getTrainerByUsername(String username, String password) {
        if (!authenticate(username, password)) {
            throw new SecurityException("Authentication failed");
        }
        return trainerDAO.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String username, String password) {
        return trainerDAO.findByUsername(username)
                .map(t -> t.getPassword().equals(password))
                .orElse(false);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        if (!authenticate(username, oldPassword)) {
            throw new SecurityException("Authentication failed");
        }
        trainerDAO.findByUsername(username).ifPresent(t -> {
            t.setPassword(newPassword);
            trainerDAO.update(t);
            log.info("Changed password for Trainer: {}", username);
        });
    }

    @Transactional
    public void activateDeactivate(String username, String password, boolean isActive) {
        if (!authenticate(username, password)) {
            throw new SecurityException("Authentication failed");
        }
        Trainer t = trainerDAO.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Trainer not found"));
        if (t.isActive() == isActive) {
            throw new IllegalStateException("Trainer is already " + (isActive ? "active" : "inactive"));
        }
        t.setActive(isActive);
        trainerDAO.update(t);
        log.info("{} Trainer: {}", isActive ? "Activated" : "De-activated", username);
    }

    @Transactional(readOnly = true)
    public Collection<Trainer> getTrainersNotAssignedToTrainee(String traineeUsername, String password) {
        if (!traineeDAO.findByUsername(traineeUsername).map(t -> t.getPassword().equals(password)).orElse(false)) {
            throw new SecurityException("Authentication failed");
        }
        return trainerDAO.findNotAssignedToTrainee(traineeUsername);
    }

    private void validateTrainer(Trainer trainer) {
        if (trainer.getFirstName() == null || trainer.getFirstName().isEmpty() ||
            trainer.getLastName() == null || trainer.getLastName().isEmpty() ||
            trainer.getSpecialization() == null) {
            throw new IllegalArgumentException("First name, Last name and Specialization are required");
        }
    }
}
