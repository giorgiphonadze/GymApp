package com.gymcrm.security;

import com.gymcrm.dao.TraineeDAO;
import com.gymcrm.dao.TrainerDAO;
import com.gymcrm.domain.User;
import com.gymcrm.domain.UserType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GymUserDetailsService implements UserDetailsService {
    private final TraineeDAO traineeDAO;
    private final TrainerDAO trainerDAO;

    public GymUserDetailsService(TraineeDAO traineeDAO, TrainerDAO trainerDAO) {
        this.traineeDAO = traineeDAO;
        this.trainerDAO = trainerDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Optional<? extends User> trainee = traineeDAO.findByUsername(username);
        if (trainee.isPresent()) {
            return toUserDetails(trainee.get(), UserType.TRAINEE);
        }
        return trainerDAO.findByUsername(username)
                .map(trainer -> toUserDetails(trainer, UserType.TRAINER))
                .orElseThrow(() -> new UsernameNotFoundException("No trainee or trainer account matches the given username"));
    }

    private static UserDetails toUserDetails(User user, UserType userType) {
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(userType.name())
                .build();
    }
}
