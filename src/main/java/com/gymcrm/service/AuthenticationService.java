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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final TraineeDAO traineeDAO;
    private final TrainerDAO trainerDAO;
    private final PasswordEncoder passwordEncoder;
    private final GymMetrics metrics;

    public AuthenticationService(TraineeDAO traineeDAO, TrainerDAO trainerDAO, PasswordEncoder passwordEncoder,
                                 GymMetrics metrics) {
        this.traineeDAO = traineeDAO;
        this.trainerDAO = trainerDAO;
        this.passwordEncoder = passwordEncoder;
        this.metrics = metrics;
    }

    public Trainee authenticateTrainee(String username, String password) {
        return verify(UserType.TRAINEE, traineeDAO.findByUsername(username), password);
    }

    public Trainer authenticateTrainer(String username, String password) {
        return verify(UserType.TRAINER, trainerDAO.findByUsername(username), password);
    }

    public boolean isValidTrainee(String username, String password) {
        return attempt(UserType.TRAINEE, traineeDAO.findByUsername(username), password).isPresent();
    }

    public boolean isValidTrainer(String username, String password) {
        return attempt(UserType.TRAINER, trainerDAO.findByUsername(username), password).isPresent();
    }

    public boolean login(String username, String password) {
        return identify(username, password).isPresent();
    }

    public Optional<UserType> identify(String username, String password) {
        Optional<Trainee> trainee = traineeDAO.findByUsername(username);
        if (trainee.isPresent()) {
            return attempt(UserType.TRAINEE, trainee, password).map(user -> UserType.TRAINEE);
        }
        Optional<Trainer> trainer = trainerDAO.findByUsername(username);
        if (trainer.isEmpty()) {
            metrics.recordAuthentication(UserType.TRAINER, false);
            log.warn("Login failed: no trainee or trainer account matches the given username");
            return Optional.empty();
        }
        return attempt(UserType.TRAINER, trainer, password).map(user -> UserType.TRAINER);
    }

    public Optional<UserType> findUserType(String username) {
        if (traineeDAO.findByUsername(username).isPresent()) {
            return Optional.of(UserType.TRAINEE);
        }
        if (trainerDAO.findByUsername(username).isPresent()) {
            return Optional.of(UserType.TRAINER);
        }
        return Optional.empty();
    }

    public Trainee requireTrainee(String username) {
        return traineeDAO.findByUsername(username)
                .orElseThrow(() -> accountNotFound(UserType.TRAINEE));
    }

    public Trainer requireTrainer(String username) {
        return trainerDAO.findByUsername(username)
                .orElseThrow(() -> accountNotFound(UserType.TRAINER));
    }

    private <T extends User> Optional<T> attempt(UserType userType, Optional<T> user, String password) {
        try {
            return Optional.of(verify(userType, user, password));
        } catch (PasswordException e) {
            return Optional.empty();
        }
    }

    private <T extends User> T verify(UserType userType, Optional<T> user, String password) {
        if (user.isEmpty()) {
            metrics.recordAuthentication(userType, false);
            log.warn("Authentication failed: {} account not found", label(userType));
            throw accountNotFound(userType);
        }
        if (!passwordMatches(user.get().getPassword(), password)) {
            metrics.recordAuthentication(userType, false);
            log.warn("Authentication failed: incorrect password for {} account", label(userType));
            throw new PasswordException("Incorrect password for " + label(userType) + " account");
        }
        metrics.recordAuthentication(userType, true);
        log.debug("Authenticated {} account", label(userType));
        return user.get();
    }

    private static PasswordException accountNotFound(UserType userType) {
        String label = label(userType);
        return new PasswordException(Character.toUpperCase(label.charAt(0)) + label.substring(1) + " account not found");
    }

    private static String label(UserType userType) {
        return userType.name().toLowerCase();
    }

    private boolean passwordMatches(String storedHash, String rawPassword) {
        if (storedHash == null || rawPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, storedHash);
    }
}
