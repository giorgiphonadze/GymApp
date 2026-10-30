package com.gymcrm.service;

import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.dao.TrainingDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.metrics.GymMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private final TrainingDAO trainingDAO;
    private final TrainerDAO trainerDAO;
    private final AuthenticationService authenticationService;
    private final GymMetrics metrics;

    public TrainingService(TrainingDAO trainingDAO, TrainerDAO trainerDAO,
                           AuthenticationService authenticationService, GymMetrics metrics) {
        this.trainingDAO = trainingDAO;
        this.trainerDAO = trainerDAO;
        this.authenticationService = authenticationService;
        this.metrics = metrics;
    }

    @Transactional
    public void createTraining(String traineeUsername, String trainerUsername,
                               String trainingName, LocalDate date, Duration duration) {
        Trainee trainee = authenticationService.requireTrainee(traineeUsername);
        Trainer trainer = trainerDAO.findByUsername(trainerUsername)
                .orElseThrow(() -> {
                    log.warn("Cannot create training: trainer {} not found", trainerUsername);
                    return new IllegalArgumentException("Trainer not found");
                });

        validateTraining(trainingName, date, duration);
        Training training = new Training(trainer, trainee,
                trainingName, trainer.getSpecialization(), date, duration);
        trainingDAO.save(training);
        metrics.recordTrainingCreated(training.getTrainingType());
        log.info("Created Training: {} for Trainee: {} and Trainer: {}",
                trainingName, traineeUsername, trainerUsername);
    }

    @Transactional(readOnly = true)
    public List<Training> getTraineeTrainings(String username, LocalDate fromDate,
                                              LocalDate toDate, String trainerName, String trainingTypeName) {
        authenticationService.requireTrainee(username);
        return trainingDAO.findTraineeTrainings(username, fromDate, toDate, trainerName, trainingTypeName);
    }

    @Transactional(readOnly = true)
    public List<Training> getTrainerTrainings(String username, LocalDate fromDate,
                                              LocalDate toDate, String traineeName) {
        authenticationService.requireTrainer(username);
        return trainingDAO.findTrainerTrainings(username, fromDate, toDate, traineeName);
    }

    @Transactional(readOnly = true)
    public List<TrainingType> getTrainingTypes() {
        return Arrays.asList(TrainingType.values());
    }

    private void validateTraining(String name, LocalDate date, Duration duration) {
        if (name == null || name.isEmpty() || date == null || duration == null
                || duration.isNegative() || duration.isZero()) {
            log.warn("Rejected training with missing name/date or non-positive duration");
            throw new IllegalArgumentException("Training name, date and duration (positive) are required");
        }
    }
}
