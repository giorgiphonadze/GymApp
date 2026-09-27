package com.gymcrm.dao;

import com.gymcrm.domain.Trainer;
import com.gymcrm.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TrainerDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainerDAO.class);
    private static final String NAMESPACE = "Trainer";

    private Storage storage;

    @Autowired
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Trainer save(Trainer trainer) {
        if (trainer.getUserId() == null) {
            trainer.setUserId(storage.nextId(NAMESPACE));
        }
        storage.put(NAMESPACE, trainer.getUserId(), trainer);
        log.debug("Saved Trainer with ID: {}", trainer.getUserId());
        return trainer;
    }

    public Optional<Trainer> findById(Long id) {
        return Optional.ofNullable((Trainer) storage.findById(NAMESPACE, id));
    }

    public Collection<Trainer> findAll() {
        return storage.findAll(NAMESPACE).stream()
                .map(obj -> (Trainer) obj)
                .collect(Collectors.toList());
    }
}
