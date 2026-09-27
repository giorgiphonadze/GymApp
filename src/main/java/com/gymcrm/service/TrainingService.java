package com.gymcrm.service;

import com.gymcrm.dao.TrainingDAO;
import com.gymcrm.domain.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDAO trainingDAO;

    @Autowired
    public void setTrainingDAO(TrainingDAO trainingDAO) {
        this.trainingDAO = trainingDAO;
    }

    public Training createTraining(Training training) {
        Training saved = trainingDAO.save(training);
        log.info("Created Training: {}", saved.getTrainingName());
        return saved;
    }

    public Optional<Training> getTraining(Long id) {
        return trainingDAO.findById(id);
    }
}
