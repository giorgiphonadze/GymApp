package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.utils.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TraineeService {
    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDAO traineeDAO;
    private TrainerDAO trainerDAO;

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) {
        this.traineeDAO = traineeDAO;
    }

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
    }

    @Transactional
    public Trainee createTrainee(Trainee trainee) {
        validateTrainee(trainee);
        Set<String> existingUsernames = UserUtils.collectUsernames(traineeDAO.findAll(), trainerDAO.findAll());
        trainee.setUsername(UserUtils.generateUsername(trainee.getFirstName(), trainee.getLastName(), existingUsernames));
        trainee.setPassword(UserUtils.generatePassword());
        trainee.setActive(true);
        Trainee saved = traineeDAO.save(trainee);
        log.info("Created Trainee: {}", saved.getUsername());
        return saved;
    }

    @Transactional
    public void updateTrainee(String username, String password, Trainee trainee) {
        if (!authenticate(username, password)) {
            throw new SecurityException("Authentication failed");
        }
        validateTrainee(trainee);
        traineeDAO.update(trainee);
        log.info("Updated Trainee: {}", trainee.getUsername());
    }

    @Transactional
    public void deleteTrainee(String username, String password) {
        if (!authenticate(username, password)) {
            throw new SecurityException("Authentication failed");
        }
        Optional<Trainee> trainee = traineeDAO.findByUsername(username);
        trainee.ifPresent(t -> {
            traineeDAO.delete(t);
            log.info("Deleted Trainee: {}", username);
        });
    }

    @Transactional(readOnly = true)
    public Optional<Trainee> getTraineeByUsername(String username, String password) {
        if (!authenticate(username, password)) {
            throw new SecurityException("Authentication failed");
        }
        return traineeDAO.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String username, String password) {
        return traineeDAO.findByUsername(username)
                .map(t -> t.getPassword().equals(password))
                .orElse(false);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        if (!authenticate(username, oldPassword)) {
            throw new SecurityException("Authentication failed");
        }
        traineeDAO.findByUsername(username).ifPresent(t -> {
            t.setPassword(newPassword);
            traineeDAO.update(t);
            log.info("Changed password for Trainee: {}", username);
        });
    }

    @Transactional
    public void activateDeactivate(String username, String password, boolean isActive) {
        if (!authenticate(username, password)) {
            throw new SecurityException("Authentication failed");
        }
        Trainee t = traineeDAO.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Trainee not found"));
        if (t.isActive() == isActive) {
            throw new IllegalStateException("Trainee is already " + (isActive ? "active" : "inactive"));
        }
        t.setActive(isActive);
        traineeDAO.update(t);
        log.info("{} Trainee: {}", isActive ? "Activated" : "De-activated", username);
    }

    @Transactional
    public void updateTrainersList(String traineeUsername, String password, List<String> trainerUsernames) {
        if (!authenticate(traineeUsername, password)) {
            throw new SecurityException("Authentication failed");
        }
        Optional<Trainee> traineeOpt = traineeDAO.findByUsername(traineeUsername);
        if (traineeOpt.isPresent()) {
            Trainee trainee = traineeOpt.get();
            Set<Trainer> trainers = trainerUsernames.stream()
                    .map(trainerDAO::findByUsername)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            trainee.setTrainers(trainers);
            traineeDAO.update(trainee);
            log.info("Updated trainers list for Trainee: {}", traineeUsername);
        }
    }

    private void validateTrainee(Trainee trainee) {
        if (trainee.getFirstName() == null || trainee.getFirstName().isEmpty() ||
            trainee.getLastName() == null || trainee.getLastName().isEmpty()) {
            throw new IllegalArgumentException("First name and Last name are required");
        }
    }
}
