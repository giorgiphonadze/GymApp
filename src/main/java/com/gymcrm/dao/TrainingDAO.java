package com.gymcrm.dao;

import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class TrainingDAO {
    private static final Logger log = LoggerFactory.getLogger(TrainingDAO.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    public Training save(Training training) {
        getCurrentSession().persist(training);
        log.debug("Saved Training with ID: {}", training.getId());
        return training;
    }

    public Optional<Training> findById(Long id) {
        log.debug("Finding Training by ID: {}", id);
        return Optional.ofNullable(getCurrentSession().get(Training.class, id));
    }

    public Collection<Training> findAll() {
        log.debug("Finding all Trainings");
        return getCurrentSession().createQuery("from Training", Training.class).list();
    }

    public List<Training> findTraineeTrainings(String username, LocalDate fromDate, LocalDate toDate, String trainerName, String trainingTypeName) {
        StringBuilder hql = new StringBuilder("select t from Training t where t.trainee.username = :username");
        if (fromDate != null) hql.append(" and t.trainingDate >= :fromDate");
        if (toDate != null) hql.append(" and t.trainingDate <= :toDate");
        if (trainerName != null) hql.append(" and t.trainer.firstName = :trainerName");
        if (trainingTypeName != null) hql.append(" and t.trainingType = :trainingType");

        Query<Training> query = getCurrentSession().createQuery(hql.toString(), Training.class);
        query.setParameter("username", username);
        if (fromDate != null) query.setParameter("fromDate", fromDate);
        if (toDate != null) query.setParameter("toDate", toDate);
        if (trainerName != null) query.setParameter("trainerName", trainerName);
        if (trainingTypeName != null) query.setParameter("trainingType", TrainingType.valueOf(trainingTypeName.toUpperCase()));

        return query.list();
    }

    public List<Training> findTrainerTrainings(String username, LocalDate fromDate, LocalDate toDate, String traineeName) {
        StringBuilder hql = new StringBuilder("select t from Training t where t.trainer.username = :username");
        if (fromDate != null) hql.append(" and t.trainingDate >= :fromDate");
        if (toDate != null) hql.append(" and t.trainingDate <= :toDate");
        if (traineeName != null) hql.append(" and t.trainee.firstName = :traineeName");

        Query<Training> query = getCurrentSession().createQuery(hql.toString(), Training.class);
        query.setParameter("username", username);
        if (fromDate != null) query.setParameter("fromDate", fromDate);
        if (toDate != null) query.setParameter("toDate", toDate);
        if (traineeName != null) query.setParameter("traineeName", traineeName);

        return query.list();
    }

}
