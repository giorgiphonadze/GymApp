package com.gymcrm.facade;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.domain.UserType;
import com.gymcrm.service.AuthenticationService;
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
    private final AuthenticationService authenticationService;

    public GymFacade(TraineeService traineeService, TrainerService trainerService, TrainingService trainingService,
                     AuthenticationService authenticationService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        this.authenticationService = authenticationService;
    }

    public Trainee createTrainee(Trainee trainee) {
        return traineeService.createTrainee(trainee);
    }

    public Trainee updateTrainee(String username, String firstName, String lastName,
                                 LocalDate dateOfBirth, String address, boolean isActive) {
        return traineeService.updateTrainee(username, firstName, lastName, dateOfBirth, address, isActive);
    }

    public void deleteTrainee(String username) {
        traineeService.deleteTrainee(username);
    }

    public Optional<Trainee> getTrainee(String username) {
        return traineeService.getTraineeByUsername(username);
    }

    public boolean authenticateTrainee(String username, String password) {
        return traineeService.authenticate(username, password);
    }

    public boolean authenticate(String username, String password) {
        return authenticationService.login(username, password);
    }

    public Optional<UserType> login(String username, String password) {
        return authenticationService.identify(username, password);
    }

    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        traineeService.changePassword(username, oldPassword, newPassword);
    }

    public void activateDeactivateTrainee(String username, boolean isActive) {
        traineeService.activateDeactivate(username, isActive);
    }

    public Trainer createTrainer(Trainer trainer) {
        return trainerService.createTrainer(trainer);
    }

    public Trainer updateTrainer(String username, String firstName, String lastName, boolean isActive) {
        return trainerService.updateTrainer(username, firstName, lastName, isActive);
    }

    public Optional<Trainer> getTrainer(String username) {
        return trainerService.getTrainerByUsername(username);
    }

    public boolean authenticateTrainer(String username, String password) {
        return trainerService.authenticate(username, password);
    }

    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        trainerService.changePassword(username, oldPassword, newPassword);
    }

    public void activateDeactivateTrainer(String username, boolean isActive) {
        trainerService.activateDeactivate(username, isActive);
    }

    public void createTraining(String traineeUsername, String trainerUsername,
                               String trainingName, LocalDate date, Duration duration) {
        trainingService.createTraining(traineeUsername, trainerUsername, trainingName, date, duration);
    }

    public List<Training> getTraineeTrainings(String username, LocalDate fromDate,
                                              LocalDate toDate, String trainerName, String trainingTypeName) {
        return trainingService.getTraineeTrainings(username, fromDate, toDate, trainerName, trainingTypeName);
    }

    public List<Training> getTrainerTrainings(String username, LocalDate fromDate,
                                              LocalDate toDate, String traineeName) {
        return trainingService.getTrainerTrainings(username, fromDate, toDate, traineeName);
    }

    public Collection<Trainer> getTrainersNotAssignedToTrainee(String traineeUsername) {
        return trainerService.getTrainersNotAssignedToTrainee(traineeUsername);
    }

    public void updateTraineeTrainers(String traineeUsername, List<String> trainerUsernames) {
        traineeService.updateTrainersList(traineeUsername, trainerUsernames);
    }

    public List<TrainingType> getTrainingTypes() {
        return trainingService.getTrainingTypes();
    }
}
