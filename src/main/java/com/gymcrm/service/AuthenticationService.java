package com.gymcrm.service;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.User;
import com.gymcrm.domain.UserType;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.metrics.GymMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final TraineeDAO traineeDAO;
    private final TrainerDAO trainerDAO;
    private final GymMetrics metrics;

    public AuthenticationService(TraineeDAO traineeDAO, TrainerDAO trainerDAO, GymMetrics metrics) {
        this.traineeDAO = traineeDAO;
        this.trainerDAO = trainerDAO;
        this.metrics = metrics;
    }

    public Trainee authenticateTrainee(String username, String password) {
        return authenticate(UserType.TRAINEE, traineeDAO.findByUsername(username), username, password)
                .orElseThrow(() -> new PasswordException("Authentication failed"));
    }

    public Trainer authenticateTrainer(String username, String password) {
        return authenticate(UserType.TRAINER, trainerDAO.findByUsername(username), username, password)
                .orElseThrow(() -> new PasswordException("Authentication failed"));
    }

    public boolean isValidTrainee(String username, String password) {
        return authenticate(UserType.TRAINEE, traineeDAO.findByUsername(username), username, password).isPresent();
    }

    public boolean isValidTrainer(String username, String password) {
        return authenticate(UserType.TRAINER, trainerDAO.findByUsername(username), username, password).isPresent();
    }

    public boolean login(String username, String password) {
        return identify(username, password).isPresent();
    }

    public Optional<UserType> identify(String username, String password) {
        Optional<Trainee> trainee = traineeDAO.findByUsername(username);
        if (trainee.isPresent()) {
            return authenticate(UserType.TRAINEE, trainee, username, password).map(user -> UserType.TRAINEE);
        }
        return authenticate(UserType.TRAINER, trainerDAO.findByUsername(username), username, password)
                .map(user -> UserType.TRAINER);
    }

    public Trainee requireTrainee(String username) {
        return traineeDAO.findByUsername(username)
                .orElseThrow(() -> new PasswordException("Authentication failed"));
    }

    public Trainer requireTrainer(String username) {
        return trainerDAO.findByUsername(username)
                .orElseThrow(() -> new PasswordException("Authentication failed"));
    }

    private <T extends User> Optional<T> authenticate(UserType userType, Optional<T> user,
                                                      String username, String password) {
        Optional<T> authenticated = user.filter(candidate -> passwordMatches(candidate.getPassword(), password));
        metrics.recordAuthentication(userType, authenticated.isPresent());
        if (authenticated.isPresent()) {
            log.debug("Authenticated {} '{}'", userType, username);
        } else {
            log.warn("Failed authentication attempt for {} '{}'", userType, username);
        }
        return authenticated;
    }

    private static boolean passwordMatches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
