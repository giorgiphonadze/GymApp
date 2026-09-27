package com.gymcrm.facade;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.service.TraineeService;
import com.gymcrm.service.TrainerService;
import com.gymcrm.service.TrainingService;
import org.springframework.stereotype.Component;

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

    public void updateTrainee(Trainee trainee) {
        traineeService.updateTrainee(trainee);
    }

    public void deleteTrainee(Long id) {
        traineeService.deleteTrainee(id);
    }

    public Optional<Trainee> getTrainee(Long id) {
        return traineeService.getTrainee(id);
    }

    public Trainer createTrainer(Trainer trainer) {
        return trainerService.createTrainer(trainer);
    }

    public void updateTrainer(Trainer trainer) {
        trainerService.updateTrainer(trainer);
    }

    public Optional<Trainer> getTrainer(Long id) {
        return trainerService.getTrainer(id);
    }

    public Training createTraining(Training training) {
        return trainingService.createTraining(training);
    }

    public Optional<Training> getTraining(Long id) {
        return trainingService.getTraining(id);
    }
}
