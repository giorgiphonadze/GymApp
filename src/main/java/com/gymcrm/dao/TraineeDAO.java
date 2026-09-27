package com.gymcrm.dao;

import com.gymcrm.domain.Trainee;
import com.gymcrm.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TraineeDAO {
    private static final Logger log = LoggerFactory.getLogger(TraineeDAO.class);
    private static final String NAMESPACE = "Trainee";

    private Storage storage;

    @Autowired
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Trainee save(Trainee trainee) {
        if (trainee.getUserId() == null) {
            trainee.setUserId(storage.nextId(NAMESPACE));
        }
        storage.put(NAMESPACE, trainee.getUserId(), trainee);
        log.debug("Saved Trainee with ID: {}", trainee.getUserId());
        return trainee;
    }

    public Optional<Trainee> findById(Long id) {
        log.debug("Finding Trainee by ID: {}", id);
        return Optional.ofNullable((Trainee) storage.findById(NAMESPACE, id));
    }

    public void delete(Long id) {
        log.info("Deleting Trainee with ID: {}", id);
        storage.remove(NAMESPACE, id);
    }

    public Collection<Trainee> findAll() {
        log.debug("Finding all Trainees");
        return storage.findAll(NAMESPACE).stream()
                .map(obj -> (Trainee) obj)
                .collect(Collectors.toList());
    }
}
