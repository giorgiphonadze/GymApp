package com.gymcrm.dao;

import com.gymcrm.domain.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public class TrainerDAO extends AbstractDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainerDAO.class);

    public Trainer save(Trainer trainer) {
        getCurrentSession().persist(trainer);
        log.debug("Saved Trainer with ID: {}", trainer.getUserId());
        return trainer;
    }

    public Trainer update(Trainer trainer) {
        return getCurrentSession().merge(trainer);
    }

    public Optional<Trainer> findById(Long id) {
        log.debug("Finding Trainer by ID: {}", id);
        return Optional.ofNullable(getCurrentSession().get(Trainer.class, id));
    }

    public Optional<Trainer> findByUsername(String username) {
        log.debug("Finding Trainer by username: {}", username);
        return getCurrentSession().createQuery("from Trainer where username = :username", Trainer.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    public Collection<Trainer> findAll() {
        log.debug("Finding all Trainers");
        return getCurrentSession().createQuery("from Trainer", Trainer.class).list();
    }

    public Collection<Trainer> findNotAssignedToTrainee(String traineeUsername) {
        log.debug("Finding Trainers not assigned to Trainee: {}", traineeUsername);
        return getCurrentSession().createQuery(
                "from Trainer t where t not in (select tr from Trainee trn join trn.trainers tr where trn.username = :traineeUsername)",
                Trainer.class)
                .setParameter("traineeUsername", traineeUsername)
                .list();
    }
}
