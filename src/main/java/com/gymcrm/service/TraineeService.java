package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.User;
import com.gymcrm.utils.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

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

    public Trainee createTrainee(Trainee trainee) {
        Set<String> existingUsernames = UserUtils.collectUsernames(traineeDAO.findAll(), trainerDAO.findAll());
        trainee.setUsername(UserUtils.generateUsername(trainee.getFirstName(), trainee.getLastName(), existingUsernames));
        trainee.setPassword(UserUtils.generatePassword());
        Trainee saved = traineeDAO.save(trainee);
        log.info("Created Trainee: {}", saved.getUsername());
        return saved;
    }

    public void updateTrainee(Trainee trainee) {
        traineeDAO.save(trainee);
        log.info("Updated Trainee: {}", trainee.getUsername());
    }

    public void deleteTrainee(Long id) {
        traineeDAO.delete(id);
        log.info("Deleted Trainee with ID: {}", id);
    }

    public Optional<Trainee> getTrainee(Long id) {
        return traineeDAO.findById(id);
    }
}
