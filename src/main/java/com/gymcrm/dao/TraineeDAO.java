package com.gymcrm.dao;

import com.gymcrm.domain.Trainee;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public class TraineeDAO {
    private static final Logger log = LoggerFactory.getLogger(TraineeDAO.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    public Trainee save(Trainee trainee) {
        getCurrentSession().persist(trainee);
        log.debug("Saved/Updated Trainee with ID: {}", trainee.getUserId());
        return trainee;
    }

    public Trainee update(Trainee trainee) {
        return getCurrentSession().merge(trainee);
    }

    public Optional<Trainee> findById(Long id) {
        log.debug("Finding Trainee by ID: {}", id);
        return Optional.ofNullable(getCurrentSession().get(Trainee.class, id));
    }

    public Optional<Trainee> findByUsername(String username) {
        log.debug("Finding Trainee by username: {}", username);
        return getCurrentSession().createQuery("from Trainee where username = :username", Trainee.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    public void delete(Trainee trainee) {
        log.info("Deleting Trainee with ID: {}", trainee.getUserId());
        getCurrentSession().remove(trainee);
    }

    public Collection<Trainee> findAll() {
        log.debug("Finding all Trainees");
        return getCurrentSession().createQuery("from Trainee", Trainee.class).list();
    }
}
