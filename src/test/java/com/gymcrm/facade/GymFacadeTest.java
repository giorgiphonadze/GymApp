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
import com.gymcrm.dto.TraineeProfileResponse;
import com.gymcrm.dto.TraineeRegistrationRequest;
import com.gymcrm.dto.TrainerInfo;
import com.gymcrm.dto.TrainerProfileResponse;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.TrainingTypeResponse;
import com.gymcrm.exception.ForbiddenException;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.security.AuthenticatedUser;
import com.gymcrm.security.JwtService;
import com.gymcrm.service.AuthenticationService;
import com.gymcrm.service.TraineeService;
import com.gymcrm.service.TrainerService;
import com.gymcrm.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GymFacadeTest {
    private static final JwtService JWT_SERVICE = new JwtService("facade-tests-secret-0123456789-abcdef", 30);

    @Mock
    private TraineeService traineeService;
    @Mock
    private TrainerService trainerService;
    @Mock
    private TrainingService trainingService;
    @Mock
    private AuthenticationService authenticationService;

    private GymFacade facade;

    @BeforeEach
    void setUp() {
        facade = new GymFacade(traineeService, trainerService, trainingService, authenticationService, JWT_SERVICE);
    }

    private static String traineeToken(String username) {
        return "Bearer " + JWT_SERVICE.generateToken(username, UserType.TRAINEE);
    }

    private static String trainerToken(String username) {
        return "Bearer " + JWT_SERVICE.generateToken(username, UserType.TRAINER);
    }

    private static Trainer trainer(String username, String firstName, String lastName) {
        Trainer trainer = new Trainer(firstName, lastName, TrainingType.YOGA);
        trainer.setUsername(username);
        return trainer;
    }

    @Test
    void loginIssuesTokenForTheAuthenticatedUserType() {
        when(authenticationService.identify("coach", "pass")).thenReturn(Optional.of(UserType.TRAINER));

        LoginResponse response = facade.login(new LoginRequest("coach", "pass"));

        assertEquals("Bearer", response.getTokenType());
        assertEquals(1800, response.getExpiresIn());
        AuthenticatedUser user = JWT_SERVICE.authenticate("Bearer " + response.getToken());
        assertEquals("coach", user.username());
        assertEquals(UserType.TRAINER, user.userType());
    }

    @Test
    void loginFailureHasDescriptiveMessage() {
        when(authenticationService.identify("user", "secret")).thenReturn(Optional.empty());

        PasswordException e = assertThrows(PasswordException.class,
                () -> facade.login(new LoginRequest("user", "secret")));

        assertEquals("Login failed: invalid username or password", e.getMessage());
    }

    @Test
    void changePasswordDispatchesByUserType() {
        when(authenticationService.findUserType("t")).thenReturn(Optional.of(UserType.TRAINEE));
        when(authenticationService.findUserType("tr")).thenReturn(Optional.of(UserType.TRAINER));

        facade.changePassword(new ChangePasswordRequest("t", "old", "new"));
        facade.changePassword(new ChangePasswordRequest("tr", "old", "new"));

        verify(traineeService).changePassword("t", "old", "new");
        verify(trainerService).changePassword("tr", "old", "new");
        verify(trainerService, never()).changePassword(eq("t"), any(), any());
    }

    @Test
    void changePasswordForUnknownUserFails() {
        when(authenticationService.findUserType("nobody")).thenReturn(Optional.empty());

        PasswordException e = assertThrows(PasswordException.class,
                () -> facade.changePassword(new ChangePasswordRequest("nobody", "old", "new")));

        assertEquals("Password change failed: invalid username or old password", e.getMessage());
        verifyNoInteractions(traineeService, trainerService);
    }

    @Test
    void changePasswordWithWrongOldPasswordKeepsReasonAsCause() {
        when(authenticationService.findUserType("t")).thenReturn(Optional.of(UserType.TRAINEE));
        doThrow(new PasswordException("Incorrect password for trainee account"))
                .when(traineeService).changePassword("t", "wrong", "new");

        PasswordException e = assertThrows(PasswordException.class,
                () -> facade.changePassword(new ChangePasswordRequest("t", "wrong", "new")));

        assertEquals("Password change failed: invalid username or old password", e.getMessage());
        assertEquals("Incorrect password for trainee account", e.getCause().getMessage());
        verifyNoInteractions(trainerService);
    }

    @Test
    void registerTraineeBuildsDomainObjectAndReturnsCredentials() {
        when(traineeService.createTrainee(any(Trainee.class))).thenAnswer(invocation -> {
            Trainee trainee = invocation.getArgument(0);
            trainee.setUsername("John.Doe");
            trainee.setPassword("generated");
            return trainee;
        });

        RegistrationResponse response = facade.registerTrainee(
                new TraineeRegistrationRequest("John", "Doe", LocalDate.of(2000, 1, 1), "Street"));

        ArgumentCaptor<Trainee> captor = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeService).createTrainee(captor.capture());
        assertEquals("John", captor.getValue().getFirstName());
        assertEquals("Street", captor.getValue().getAddress());
        assertEquals(new RegistrationResponse("John.Doe", "generated"), response);
    }

    @Test
    void getTraineeProfileMapsTraineeAndTrainers() {
        Trainee trainee = new Trainee("John", "Doe", null, "Street");
        trainee.setUsername("John.Doe");
        trainee.setActive(true);
        trainee.setTrainers(Set.of(trainer("Jane.Doe", "Jane", "Doe")));
        when(traineeService.getTraineeByUsername("John.Doe")).thenReturn(Optional.of(trainee));

        TraineeProfileResponse profile = facade.getTraineeProfile(traineeToken("John.Doe"), "John.Doe");

        assertEquals("John", profile.getFirstName());
        assertTrue(profile.isActive());
        assertEquals(List.of(new TrainerInfo("Jane.Doe", "Jane", "Doe", TrainingType.YOGA)), profile.getTrainers());
    }

    @Test
    void getTrainerProfileMapsMissingTraineesToEmptyList() {
        Trainer trainer = trainer("Jane.Doe", "Jane", "Doe");
        when(trainerService.getTrainerByUsername("Jane.Doe")).thenReturn(Optional.of(trainer));

        TrainerProfileResponse profile = facade.getTrainerProfile(trainerToken("Jane.Doe"), "Jane.Doe");

        assertEquals(TrainingType.YOGA, profile.getSpecialization());
        assertEquals(List.of(), profile.getTrainees());
    }

    @Test
    void missingAuthorizationHeaderIsRejectedBeforeReachingServices() {
        PasswordException e = assertThrows(PasswordException.class,
                () -> facade.getTraineeProfile(null, "John.Doe"));

        assertEquals("Authorization header is missing", e.getMessage());
        verifyNoInteractions(traineeService);
    }

    @Test
    void basicCredentialsAreRejected() {
        PasswordException e = assertThrows(PasswordException.class,
                () -> facade.getTraineeProfile("Basic Sm9obi5Eb2U6cGFzcw==", "John.Doe"));

        assertEquals("Authorization header must use the Bearer scheme", e.getMessage());
    }

    @Test
    void tokenOfAnotherTraineeIsForbidden() {
        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> facade.deleteTraineeProfile(traineeToken("Someone.Else"), "John.Doe"));

        assertEquals("Access denied: the token does not belong to the requested trainee account", e.getMessage());
        assertFalse(e.getMessage().contains("Someone.Else") || e.getMessage().contains("John.Doe"));
        verifyNoInteractions(traineeService);
    }

    @Test
    void trainerTokenCannotAccessTraineeResources() {
        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> facade.getTraineeProfile(trainerToken("John.Doe"), "John.Doe"));

        assertEquals("Access denied: this resource requires a trainee token, but a trainer token was provided",
                e.getMessage());
    }

    @Test
    void traineeTokenCannotAccessTrainerResources() {
        assertThrows(ForbiddenException.class,
                () -> facade.activateDeactivateTrainer(traineeToken("Jane.Doe"), "Jane.Doe",
                        new ActivateDeactivateRequest("Jane.Doe", false)));

        verifyNoInteractions(trainerService);
    }

    @Test
    void updateTraineeTrainersReturnsUpdatedList() {
        Trainee trainee = new Trainee();
        trainee.setTrainers(Set.of(trainer("Jane.Doe", "Jane", "Doe")));
        when(traineeService.getTraineeByUsername("John.Doe")).thenReturn(Optional.of(trainee));

        List<TrainerInfo> trainers = facade.updateTraineeTrainers(traineeToken("John.Doe"), "John.Doe",
                List.of("Jane.Doe"));

        verify(traineeService).updateTrainersList("John.Doe", List.of("Jane.Doe"));
        assertEquals("Jane.Doe", trainers.get(0).getUsername());
    }

    @Test
    void traineeTrainingsAreMappedWithTrainerName() {
        Trainee trainee = new Trainee("John", "Doe", null, null);
        Training training = new Training(trainer("Jane.Doe", "Jane", "Doe"), trainee, "Morning",
                TrainingType.YOGA, LocalDate.of(2030, 1, 1), Duration.ofMinutes(45));
        when(trainingService.getTraineeTrainings("John.Doe", null, null, null, null)).thenReturn(List.of(training));

        List<TrainingResponse> trainings = facade.getTraineeTrainings(traineeToken("John.Doe"), "John.Doe",
                null, null, null, null);

        assertEquals(1, trainings.size());
        assertEquals("Jane Doe", trainings.get(0).getTrainerName());
        assertEquals(45, trainings.get(0).getTrainingDuration());
        assertNull(trainings.get(0).getTraineeName());
    }

    @Test
    void trainerTrainingsAreMappedWithTraineeName() {
        Trainee trainee = new Trainee("John", "Doe", null, null);
        Training training = new Training(trainer("Jane.Doe", "Jane", "Doe"), trainee, "Morning",
                TrainingType.YOGA, LocalDate.of(2030, 1, 1), Duration.ofMinutes(30));
        when(trainingService.getTrainerTrainings("Jane.Doe", null, null, "John")).thenReturn(List.of(training));

        List<TrainingResponse> trainings = facade.getTrainerTrainings(trainerToken("Jane.Doe"), "Jane.Doe",
                null, null, "John");

        assertEquals("John Doe", trainings.get(0).getTraineeName());
        assertNull(trainings.get(0).getTrainerName());
    }

    @Test
    void addTrainingRequiresTokenOfTheTrainee() {
        AddTrainingRequest request = new AddTrainingRequest("trainee", "trainer", "workout",
                LocalDate.of(2030, 1, 1), 60);

        facade.addTraining(traineeToken("trainee"), request);
        verify(trainingService).createTraining("trainee", "trainer", "workout",
                LocalDate.of(2030, 1, 1), Duration.ofMinutes(60));

        assertThrows(ForbiddenException.class, () -> facade.addTraining(trainerToken("trainee"), request));
        assertThrows(ForbiddenException.class, () -> facade.addTraining(traineeToken("someone-else"), request));
        verifyNoMoreInteractions(trainingService);
    }

    @Test
    void trainingTypesAreAvailableToAnyAuthenticatedUser() {
        when(trainingService.getTrainingTypes()).thenReturn(List.of(TrainingType.YOGA));

        List<TrainingTypeResponse> types = facade.getTrainingTypes(trainerToken("anyone"));

        assertEquals("Yoga", types.get(0).getTrainingType());
        assertEquals((long) TrainingType.YOGA.ordinal() + 1, types.get(0).getTrainingTypeId());
        assertEquals(1, facade.getTrainingTypes(traineeToken("someone")).size());
    }

    @Test
    void trainingTypesRejectInvalidToken() {
        PasswordException e = assertThrows(PasswordException.class,
                () -> facade.getTrainingTypes("Bearer not-a-real-token"));

        assertEquals("Token is malformed", e.getMessage());
        verifyNoInteractions(trainingService);
    }
}
