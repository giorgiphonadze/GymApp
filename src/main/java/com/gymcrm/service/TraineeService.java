package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.UserType;
import com.gymcrm.metrics.GymMetrics;
import com.gymcrm.utils.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TraineeService {
    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private final TraineeDAO traineeDAO;
    private final TrainerDAO trainerDAO;
    private final AuthenticationService authenticationService;
    private final GymMetrics metrics;

    public TraineeService(TraineeDAO traineeDAO, TrainerDAO trainerDAO,
                          AuthenticationService authenticationService, GymMetrics metrics) {
        this.traineeDAO = traineeDAO;
        this.trainerDAO = trainerDAO;
        this.authenticationService = authenticationService;
        this.metrics = metrics;
    }

    @Transactional
    public Trainee createTrainee(Trainee trainee) {
        log.debug("Creating trainee {} {}", trainee.getFirstName(), trainee.getLastName());
        validateTrainee(trainee);
        Set<String> existingUsernames = UserUtils.collectUsernames(traineeDAO.findAll(), trainerDAO.findAll());
        trainee.setUsername(UserUtils.generateUsername(trainee.getFirstName(), trainee.getLastName(), existingUsernames));
        trainee.setPassword(UserUtils.generatePassword());
        trainee.setActive(true);
        Trainee saved = traineeDAO.save(trainee);
        metrics.recordRegistration(UserType.TRAINEE);
        log.info("Created Trainee: {}", saved.getUsername());
        return saved;
    }

    @Transactional
    public Trainee updateTrainee(String username, String firstName, String lastName,
                                 LocalDate dateOfBirth, String address, boolean isActive) {
        Trainee trainee = authenticationService.requireTrainee(username);
        validateNames(firstName, lastName);
        trainee.setFirstName(firstName);
        trainee.setLastName(lastName);
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);
        trainee.setActive(isActive);
        Trainee updated = traineeDAO.update(trainee);
        log.info("Updated Trainee: {}", username);
        return updated;
    }

    @Transactional
    public void deleteTrainee(String username) {
        Trainee trainee = authenticationService.requireTrainee(username);
        traineeDAO.delete(trainee);
        log.info("Deleted Trainee: {}", username);
    }

    @Transactional(readOnly = true)
    public Optional<Trainee> getTraineeByUsername(String username) {
        return Optional.of(authenticationService.requireTrainee(username));
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String username, String password) {
        return authenticationService.isValidTrainee(username, password);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Trainee trainee = authenticationService.authenticateTrainee(username, oldPassword);
        trainee.setPassword(newPassword);
        traineeDAO.update(trainee);
        log.info("Changed password for Trainee: {}", username);
    }

    @Transactional
    public void activateDeactivate(String username, boolean isActive) {
        Trainee trainee = authenticationService.requireTrainee(username);
        if (trainee.isActive() == isActive) {
            log.warn("Trainee {} is already {}", username, isActive ? "active" : "inactive");
            throw new IllegalStateException("Trainee is already " + (isActive ? "active" : "inactive"));
        }
        trainee.setActive(isActive);
        traineeDAO.update(trainee);
        log.info("{} Trainee: {}", isActive ? "Activated" : "De-activated", username);
    }

    @Transactional
    public void updateTrainersList(String traineeUsername, List<String> trainerUsernames) {
        Trainee trainee = authenticationService.requireTrainee(traineeUsername);
        Set<Trainer> trainers = trainerUsernames.stream()
                .map(trainerDAO::findByUsername)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        if (trainers.size() < trainerUsernames.size()) {
            log.warn("Some of the {} requested trainers for Trainee {} were not found", trainerUsernames.size(), traineeUsername);
        }
        trainee.setTrainers(trainers);
        traineeDAO.update(trainee);
        log.info("Updated trainers list for Trainee: {}", traineeUsername);
    }

    private void validateTrainee(Trainee trainee) {
        validateNames(trainee.getFirstName(), trainee.getLastName());
    }

    private void validateNames(String firstName, String lastName) {
        if (firstName == null || firstName.isEmpty() || lastName == null || lastName.isEmpty()) {
            log.warn("Rejected trainee without first or last name");
            throw new IllegalArgumentException("First name and Last name are required");
        }
    }
}
