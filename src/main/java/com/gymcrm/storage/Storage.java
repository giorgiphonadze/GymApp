package com.gymcrm.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class Storage {

    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    private Map<Long, Object> traineeMap;
    private Map<Long, Object> trainerMap;
    private Map<Long, Object> trainingMap;

    private final Map<String, AtomicLong> idSequences = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("traineeMap")
    public void setTraineeMap(Map<Long, Object> traineeMap) {
        this.traineeMap = traineeMap;
    }

    @Autowired
    @Qualifier("trainerMap")
    public void setTrainerMap(Map<Long, Object> trainerMap) {
        this.trainerMap = trainerMap;
    }

    @Autowired
    @Qualifier("trainingMap")
    public void setTrainingMap(Map<Long, Object> trainingMap) {
        this.trainingMap = trainingMap;
    }

    public Map<Long, Object> getNamespace(String namespace) {
        return switch (namespace) {
            case "Trainee" -> traineeMap;
            case "Trainer" -> trainerMap;
            case "Training" -> trainingMap;
            default -> throw new IllegalArgumentException("Unknown namespace: " + namespace);
        };
    }

    public Collection<Object> findAll(String namespace) {
        return getNamespace(namespace).values();
    }

    public Object findById(String namespace, Long id) {
        return getNamespace(namespace).get(id);
    }

    public long nextId(String namespace) {
        return idSequences.computeIfAbsent(namespace, ns -> new AtomicLong(0)).incrementAndGet();
    }

    public void put(String namespace, Long id, Object entity) {
        getNamespace(namespace).put(id, entity);
        idSequences.computeIfAbsent(namespace, ns -> new AtomicLong(0))
                .updateAndGet(current -> Math.max(current, id));
        log.debug("Stored entity under namespace '{}' with id {}", namespace, id);
    }

    public void remove(String namespace, Long id) {
        getNamespace(namespace).remove(id);
        log.debug("Removed entity under namespace '{}' with id {}", namespace, id);
    }
}
