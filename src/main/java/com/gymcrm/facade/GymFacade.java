package com.gymcrm.facade;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.service.TraineeService;
import com.gymcrm.service.TrainerService;
import com.gymcrm.service.TrainingService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class GymFacade {
    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public GymFacade(TraineeService traineeService, TrainerService trainerService, TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    public Trainee createTrainee(Trainee trainee) {
        return traineeService.createTrainee(trainee);
    }

    public void updateTrainee(String username, String password, Trainee trainee) {
        traineeService.updateTrainee(username, password, trainee);
    }

    public void deleteTrainee(String username, String password) {
        traineeService.deleteTrainee(username, password);
    }

    public Optional<Trainee> getTrainee(String username, String password) {
        return traineeService.getTraineeByUsername(username, password);
    }

    public boolean authenticateTrainee(String username, String password) {
        return traineeService.authenticate(username, password);
    }

    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        traineeService.changePassword(username, oldPassword, newPassword);
    }

    public void activateDeactivateTrainee(String username, String password, boolean isActive) {
        traineeService.activateDeactivate(username, password, isActive);
    }

    public Trainer createTrainer(Trainer trainer) {
        return trainerService.createTrainer(trainer);
    }

    public void updateTrainer(String username, String password, Trainer trainer) {
        trainerService.updateTrainer(username, password, trainer);
    }

    public Optional<Trainer> getTrainer(String username, String password) {
        return trainerService.getTrainerByUsername(username, password);
    }

    public boolean authenticateTrainer(String username, String password) {
        return trainerService.authenticate(username, password);
    }

    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        trainerService.changePassword(username, oldPassword, newPassword);
    }

    public void activateDeactivateTrainer(String username, String password, boolean isActive) {
        trainerService.activateDeactivate(username, password, isActive);
    }

    public void createTraining(String traineeUsername, String password, String trainerUsername, String trainingName, LocalDate date, Duration duration) {
        trainingService.createTraining(traineeUsername, password, trainerUsername, trainingName, date, duration);
    }

    public List<Training> getTraineeTrainings(String username, String password, LocalDate fromDate, LocalDate toDate, String trainerName, String trainingTypeName) {
        return trainingService.getTraineeTrainings(username, password, fromDate, toDate, trainerName, trainingTypeName);
    }

    public List<Training> getTrainerTrainings(String username, String password, LocalDate fromDate, LocalDate toDate, String traineeName) {
        return trainingService.getTrainerTrainings(username, password, fromDate, toDate, traineeName);
    }

    public Collection<Trainer> getTrainersNotAssignedToTrainee(String traineeUsername, String password) {
        return trainerService.getTrainersNotAssignedToTrainee(traineeUsername, password);
    }

    public void updateTraineeTrainers(String traineeUsername, String password, List<String> trainerUsernames) {
        traineeService.updateTrainersList(traineeUsername, password, trainerUsernames);
    }

    public List<TrainingType> getTrainingTypes() {
        return trainingService.getTrainingTypes();
    }
}
