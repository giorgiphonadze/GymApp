package com.gymcrm.dao;

import com.gymcrm.domain.Training;
import com.gymcrm.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TrainingDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainingDAO.class);
    private static final String NAMESPACE = "Training";

    private Storage storage;

    @Autowired
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Training save(Training training) {
        if (training.getId() == null) {
            training.setId(storage.nextId(NAMESPACE));
        }
        storage.put(NAMESPACE, training.getId(), training);
        log.debug("Saved Training with ID: {}", training.getId());
        return training;
    }

    public Optional<Training> findById(Long id) {
        log.debug("Finding Training by ID: {}", id);
        return Optional.ofNullable((Training) storage.findById(NAMESPACE, id));
    }

    public Collection<Training> findAll() {
        log.debug("Finding all Trainings");
        return storage.findAll(NAMESPACE).stream()
                .map(obj -> (Training) obj)
                .collect(Collectors.toList());
    }
}
