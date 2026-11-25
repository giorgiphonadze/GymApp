package com.gymcrm.facade;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.domain.UserType;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.AddTrainingRequest;
import com.gymcrm.dto.ChangePasswordRequest;
import com.gymcrm.dto.LoginRequest;
import com.gymcrm.dto.LoginResponse;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TraineeInfo;
import com.gymcrm.dto.TraineeProfileResponse;
import com.gymcrm.dto.TraineeRegistrationRequest;
import com.gymcrm.dto.TrainerInfo;
import com.gymcrm.dto.TrainerProfileResponse;
import com.gymcrm.dto.TrainerRegistrationRequest;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.TrainingTypeResponse;
import com.gymcrm.dto.UpdateTraineeRequest;
import com.gymcrm.dto.UpdateTrainerRequest;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.security.AuthenticatedUser;
import com.gymcrm.security.JwtService;
import com.gymcrm.security.LoginService;
import com.gymcrm.service.AuthenticationService;
import com.gymcrm.service.CreatedUser;
import com.gymcrm.service.TraineeService;
import com.gymcrm.service.TrainerService;
import com.gymcrm.service.TrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class GymFacade {
    private static final Logger log = LoggerFactory.getLogger(GymFacade.class);
    private static final String PASSWORD_CHANGE_FAILED = "Password change failed: invalid username or old password";

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;
    private final AuthenticationService authenticationService;
    private final LoginService loginService;
    private final JwtService jwtService;

    public GymFacade(TraineeService traineeService, TrainerService trainerService, TrainingService trainingService,
                     AuthenticationService authenticationService, LoginService loginService, JwtService jwtService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        this.authenticationService = authenticationService;
        this.loginService = loginService;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        AuthenticatedUser user = loginService.login(request.getUsername(), request.getPassword());
        String token = jwtService.generateToken(user.username(), user.userType());
        return new LoginResponse(token, JwtService.TOKEN_TYPE, jwtService.getExpiresInSeconds());
    }

    public void changePassword(AuthenticatedUser user, ChangePasswordRequest request) {
        user.requireSelf(user.userType(), request.getUsername());
        UserType userType = user.userType();
        try {
            if (userType == UserType.TRAINEE) {
                changeTraineePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
            } else {
                changeTrainerPassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
            }
        } catch (PasswordException e) {
            log.warn("Password change rejected for {} account: {}", userType.name().toLowerCase(), e.getMessage());
            throw new PasswordException(PASSWORD_CHANGE_FAILED, e);
        }
    }

    public RegistrationResponse registerTrainee(TraineeRegistrationRequest request) {
        CreatedUser<Trainee> created = createTrainee(new Trainee(request.getFirstName(), request.getLastName(),
                request.getDateOfBirth(), request.getAddress()));
        return toRegistrationResponse(created, UserType.TRAINEE);
    }

    public TraineeProfileResponse getTraineeProfile(AuthenticatedUser user, String username) {
        authorize(user, UserType.TRAINEE, username);
        return toTraineeProfileResponse(requireTrainee(username));
    }

    public TraineeProfileResponse updateTraineeProfile(AuthenticatedUser user, String username,
                                                       UpdateTraineeRequest request) {
        authorize(user, UserType.TRAINEE, username);
        Trainee updated = updateTrainee(username, request.getFirstName(), request.getLastName(),
                request.getDateOfBirth(), request.getAddress(), request.getIsActive());
        return toTraineeProfileResponse(updated);
    }

    public void deleteTraineeProfile(AuthenticatedUser user, String username) {
        authorize(user, UserType.TRAINEE, username);
        deleteTrainee(username);
    }

    public List<TrainerInfo> updateTraineeTrainers(AuthenticatedUser user, String username,
                                                   List<String> trainerUsernames) {
        authorize(user, UserType.TRAINEE, username);
        updateTraineeTrainers(username, trainerUsernames);
        return toTrainerInfos(requireTrainee(username).getTrainers());
    }

    public List<TrainingResponse> getTraineeTrainings(AuthenticatedUser user, String username, LocalDate fromDate,
                                                      LocalDate toDate, String trainerName, String trainingTypeName) {
        authorize(user, UserType.TRAINEE, username);
        return getTraineeTrainings(username, fromDate, toDate, trainerName, trainingTypeName).stream()
                .map(training -> TrainingResponse.builder()
                        .trainingName(training.getTrainingName())
                        .trainingDate(training.getTrainingDate())
                        .trainingType(training.getTrainingType())
                        .trainingDuration((int) training.getTrainingDuration().toMinutes())
                        .trainerName(training.getTrainer().getFirstName() + " " + training.getTrainer().getLastName())
                        .build())
                .toList();
    }

    public List<TrainerInfo> getUnassignedTrainers(AuthenticatedUser user, String username) {
        authorize(user, UserType.TRAINEE, username);
        return toTrainerInfos(getTrainersNotAssignedToTrainee(username));
    }

    public void activateDeactivateTrainee(AuthenticatedUser user, String username, ActivateDeactivateRequest request) {
        authorize(user, UserType.TRAINEE, username);
        activateDeactivateTrainee(username, request.getIsActive());
    }

    public RegistrationResponse registerTrainer(TrainerRegistrationRequest request) {
        CreatedUser<Trainer> created = createTrainer(new Trainer(request.getFirstName(), request.getLastName(),
                request.getSpecialization()));
        return toRegistrationResponse(created, UserType.TRAINER);
    }

    public TrainerProfileResponse getTrainerProfile(AuthenticatedUser user, String username) {
        authorize(user, UserType.TRAINER, username);
        return toTrainerProfileResponse(requireTrainer(username));
    }

    public TrainerProfileResponse updateTrainerProfile(AuthenticatedUser user, String username,
                                                       UpdateTrainerRequest request) {
        authorize(user, UserType.TRAINER, username);
        Trainer updated = updateTrainer(username, request.getFirstName(), request.getLastName(), request.getIsActive());
        return toTrainerProfileResponse(updated);
    }

    public List<TrainingResponse> getTrainerTrainings(AuthenticatedUser user, String username, LocalDate fromDate,
                                                      LocalDate toDate, String traineeName) {
        authorize(user, UserType.TRAINER, username);
        return getTrainerTrainings(username, fromDate, toDate, traineeName).stream()
                .map(training -> TrainingResponse.builder()
                        .trainingName(training.getTrainingName())
                        .trainingDate(training.getTrainingDate())
                        .trainingType(training.getTrainingType())
                        .trainingDuration((int) training.getTrainingDuration().toMinutes())
                        .traineeName(training.getTrainee().getFirstName() + " " + training.getTrainee().getLastName())
                        .build())
                .toList();
    }

    public void activateDeactivateTrainer(AuthenticatedUser user, String username, ActivateDeactivateRequest request) {
        authorize(user, UserType.TRAINER, username);
        activateDeactivateTrainer(username, request.getIsActive());
    }

    public void addTraining(AuthenticatedUser user, AddTrainingRequest request) {
        authorize(user, UserType.TRAINEE, request.getTraineeUsername());
        createTraining(request.getTraineeUsername(), request.getTrainerUsername(), request.getTrainingName(),
                request.getTrainingDate(), Duration.ofMinutes(request.getTrainingDuration()));
    }

    public List<TrainingTypeResponse> listTrainingTypes() {
        return getTrainingTypes().stream()
                .map(trainingType -> new TrainingTypeResponse(trainingType.getTrainingTypeName(),
                        (long) trainingType.ordinal() + 1))
                .toList();
    }

    public CreatedUser<Trainee> createTrainee(Trainee trainee) {
        return traineeService.createTrainee(trainee);
    }

    public Trainee updateTrainee(String username, String firstName, String lastName,
                                 LocalDate dateOfBirth, String address, boolean isActive) {
        return traineeService.updateTrainee(username, firstName, lastName, dateOfBirth, address, isActive);
    }

    public void deleteTrainee(String username) {
        traineeService.deleteTrainee(username);
    }

    public Optional<Trainee> getTrainee(String username) {
        return traineeService.getTraineeByUsername(username);
    }

    public boolean authenticateTrainee(String username, String password) {
        return traineeService.authenticate(username, password);
    }

    public boolean authenticate(String username, String password) {
        return authenticationService.login(username, password);
    }

    public Optional<UserType> login(String username, String password) {
        return authenticationService.identify(username, password);
    }

    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        traineeService.changePassword(username, oldPassword, newPassword);
    }

    public void activateDeactivateTrainee(String username, boolean isActive) {
        traineeService.activateDeactivate(username, isActive);
    }

    public CreatedUser<Trainer> createTrainer(Trainer trainer) {
        return trainerService.createTrainer(trainer);
    }

    public Trainer updateTrainer(String username, String firstName, String lastName, boolean isActive) {
        return trainerService.updateTrainer(username, firstName, lastName, isActive);
    }

    public Optional<Trainer> getTrainer(String username) {
        return trainerService.getTrainerByUsername(username);
    }

    public boolean authenticateTrainer(String username, String password) {
        return trainerService.authenticate(username, password);
    }

    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        trainerService.changePassword(username, oldPassword, newPassword);
    }

    public void activateDeactivateTrainer(String username, boolean isActive) {
        trainerService.activateDeactivate(username, isActive);
    }

    public void createTraining(String traineeUsername, String trainerUsername,
                               String trainingName, LocalDate date, Duration duration) {
        trainingService.createTraining(traineeUsername, trainerUsername, trainingName, date, duration);
    }

    public List<Training> getTraineeTrainings(String username, LocalDate fromDate,
                                              LocalDate toDate, String trainerName, String trainingTypeName) {
        return trainingService.getTraineeTrainings(username, fromDate, toDate, trainerName, trainingTypeName);
    }

    public List<Training> getTrainerTrainings(String username, LocalDate fromDate,
                                              LocalDate toDate, String traineeName) {
        return trainingService.getTrainerTrainings(username, fromDate, toDate, traineeName);
    }

    public Collection<Trainer> getTrainersNotAssignedToTrainee(String traineeUsername) {
        return trainerService.getTrainersNotAssignedToTrainee(traineeUsername);
    }

    public void updateTraineeTrainers(String traineeUsername, List<String> trainerUsernames) {
        traineeService.updateTrainersList(traineeUsername, trainerUsernames);
    }

    public List<TrainingType> getTrainingTypes() {
        return trainingService.getTrainingTypes();
    }

    private static void authorize(AuthenticatedUser user, UserType expectedType, String username) {
        user.requireSelf(expectedType, username);
    }

    private RegistrationResponse toRegistrationResponse(CreatedUser<?> created, UserType userType) {
        String username = created.user().getUsername();
        return new RegistrationResponse(username, created.rawPassword(),
                jwtService.generateToken(username, userType), JwtService.TOKEN_TYPE, jwtService.getExpiresInSeconds());
    }

    private Trainee requireTrainee(String username) {
        return getTrainee(username).orElseThrow(() -> new PasswordException("Trainee account not found"));
    }

    private Trainer requireTrainer(String username) {
        return getTrainer(username).orElseThrow(() -> new PasswordException("Trainer account not found"));
    }

    private static TraineeProfileResponse toTraineeProfileResponse(Trainee trainee) {
        return TraineeProfileResponse.builder()
                .username(trainee.getUsername())
                .firstName(trainee.getFirstName())
                .lastName(trainee.getLastName())
                .dateOfBirth(trainee.getDateOfBirth())
                .address(trainee.getAddress())
                .isActive(trainee.isActive())
                .trainers(toTrainerInfos(trainee.getTrainers()))
                .build();
    }

    private static TrainerProfileResponse toTrainerProfileResponse(Trainer trainer) {
        return TrainerProfileResponse.builder()
                .username(trainer.getUsername())
                .firstName(trainer.getFirstName())
                .lastName(trainer.getLastName())
                .specialization(trainer.getSpecialization())
                .isActive(trainer.isActive())
                .trainees(trainer.getTrainees() != null ? trainer.getTrainees().stream()
                        .map(trainee -> new TraineeInfo(trainee.getUsername(), trainee.getFirstName(), trainee.getLastName()))
                        .toList() : List.of())
                .build();
    }

    private static List<TrainerInfo> toTrainerInfos(Collection<Trainer> trainers) {
        return trainers.stream()
                .map(trainer -> new TrainerInfo(trainer.getUsername(), trainer.getFirstName(),
                        trainer.getLastName(), trainer.getSpecialization()))
                .toList();
    }
}
