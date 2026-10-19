package com.gymcrm.service;

import com.gymcrm.exception.PasswordException;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.dao.TrainingDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDAO trainingDAO;
    private TraineeDAO traineeDAO;
    private TrainerDAO trainerDAO;

    @Autowired
    public void setTrainingDAO(TrainingDAO trainingDAO) {
        this.trainingDAO = trainingDAO;
    }

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) {
        this.traineeDAO = traineeDAO;
    }

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
    }

    private Trainee authenticateTrainee(String password, String traineeUsername) {
        return traineeDAO.findByUsername(traineeUsername)
                .filter(trainee -> trainee.getPassword().equals(password))
                .orElseThrow(() -> new PasswordException("Authentication failed"));
    }

    private void authenticateTrainer(String password, String trainerUsername) {
        if (!trainerDAO.findByUsername(trainerUsername)
                .map(trainer -> trainer.getPassword().equals(password)).orElse(false)) {
            throw new PasswordException("Authentication failed");
        }
    }

    @Transactional
    public void createTraining(String traineeUsername, String password, String trainerUsername,
                               String trainingName, LocalDate date, Duration duration) {
        Trainee trainee = authenticateTrainee(password, traineeUsername);
        Trainer trainer = trainerDAO.findByUsername(trainerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Trainer not found"));

        validateTraining(trainingName, date, duration);
        Training training = new Training(trainer, trainee,
                trainingName, trainer.getSpecialization(), date, duration);
        trainingDAO.save(training);
        log.info("Created Training: {} for Trainee: {} and Trainer: {}",
                trainingName, traineeUsername, trainerUsername);
    }

    @Transactional(readOnly = true)
    public List<Training> getTraineeTrainings(String username, String password, LocalDate fromDate,
                                              LocalDate toDate, String trainerName, String trainingTypeName) {
        authenticateTrainee(password, username);
        return trainingDAO.findTraineeTrainings(username, fromDate, toDate, trainerName, trainingTypeName);
    }

    @Transactional(readOnly = true)
    public List<Training> getTrainerTrainings(String username, String password, LocalDate fromDate,
                                              LocalDate toDate, String traineeName) {
        authenticateTrainer(password, username);
        return trainingDAO.findTrainerTrainings(username, fromDate, toDate, traineeName);
    }

    @Transactional(readOnly = true)
    public List<TrainingType> getTrainingTypes() {
        return Arrays.asList(TrainingType.values());
    }

    private void validateTraining(String name, LocalDate date, Duration duration) {
        if (name == null || name.isEmpty() || date == null || duration == null
                || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Training name, date and duration (positive) are required");
        }
    }
}