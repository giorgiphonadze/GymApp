package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.User;
import com.gymcrm.utils.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Trainer createTrainer(Trainer trainer) {
        Set<String> existingUsernames = getAllUsernames();
        trainer.setUsername(UserUtils.generateUsername(trainer.getFirstName(), trainer.getLastName(), existingUsernames));
        trainer.setPassword(UserUtils.generatePassword());
        Trainer saved = trainerDAO.save(trainer);
        log.info("Created Trainer: {}", saved.getUsername());
        return saved;
    }

    public void updateTrainer(Trainer trainer) {
        trainerDAO.save(trainer);
        log.info("Updated Trainer: {}", trainer.getUsername());
    }

    public Optional<Trainer> getTrainer(Long id) {
        return trainerDAO.findById(id);
    }

    private Set<String> getAllUsernames() {
        Set<String> usernames = trainerDAO.findAll().stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());
        usernames.addAll(traineeDAO.findAll().stream()
                .map(User::getUsername)
                .collect(Collectors.toSet()));
        return usernames;
    }
}
