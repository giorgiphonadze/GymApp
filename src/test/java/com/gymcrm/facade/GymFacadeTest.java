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
import com.gymcrm.dto.TrainerRegistrationRequest;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.TrainingTypeResponse;
import com.gymcrm.exception.ForbiddenException;
import com.gymcrm.exception.LoginBlockedException;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.security.AuthenticatedUser;
import com.gymcrm.security.JwtService;
import com.gymcrm.security.LoginService;
import com.gymcrm.security.VerifiedToken;
import com.gymcrm.service.AuthenticationService;
import com.gymcrm.service.CreatedUser;
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
    @Mock
    private LoginService loginService;

    private GymFacade facade;

    @BeforeEach
    void setUp() {
        facade = new GymFacade(traineeService, trainerService, trainingService, authenticationService, loginService,
                JWT_SERVICE);
    }

    private static AuthenticatedUser trainee(String username) {
        return new AuthenticatedUser(username, UserType.TRAINEE);
    }

    private static AuthenticatedUser trainer(String username) {
        return new AuthenticatedUser(username, UserType.TRAINER);
    }

    private static Trainer trainer(String username, String firstName, String lastName) {
        Trainer trainer = new Trainer(firstName, lastName, TrainingType.YOGA);
        trainer.setUsername(username);
        return trainer;
    }

    @Test
    void loginIssuesTokenForTheAuthenticatedUserType() {
        when(loginService.login("coach", "pass")).thenReturn(trainer("coach"));

        LoginResponse response = facade.login(new LoginRequest("coach", "pass"));

        assertEquals("Bearer", response.getTokenType());
        assertEquals(1800, response.getExpiresIn());
        VerifiedToken token = JWT_SERVICE.verify(response.getToken());
        assertEquals(trainer("coach"), token.user());
    }

    @Test
    void loginFailurePropagates() {
        when(loginService.login("user", "secret"))
                .thenThrow(new PasswordException("Login failed: invalid username or password"));

        PasswordException e = assertThrows(PasswordException.class,
                () -> facade.login(new LoginRequest("user", "secret")));

        assertEquals("Login failed: invalid username or password", e.getMessage());
    }

    @Test
    void blockedLoginPropagates() {
        when(loginService.login("user", "secret"))
                .thenThrow(new LoginBlockedException("Too many failed login attempts", Duration.ofMinutes(5)));

        assertThrows(LoginBlockedException.class, () -> facade.login(new LoginRequest("user", "secret")));
    }

    @Test
    void changePasswordDispatchesByUserTypeOfToken() {
        facade.changePassword(trainee("t"), new ChangePasswordRequest("t", "old", "new"));
        facade.changePassword(trainer("tr"), new ChangePasswordRequest("tr", "old", "new"));

        verify(traineeService).changePassword("t", "old", "new");
        verify(trainerService).changePassword("tr", "old", "new");
        verify(trainerService, never()).changePassword(eq("t"), any(), any());
    }

    @Test
    void changePasswordOfAnotherUserIsForbidden() {
        assertThrows(ForbiddenException.class,
                () -> facade.changePassword(trainee("t"), new ChangePasswordRequest("other", "old", "new")));

        verifyNoInteractions(traineeService, trainerService);
    }

    @Test
    void changePasswordWithWrongOldPasswordKeepsReasonAsCause() {
        doThrow(new PasswordException("Incorrect password for trainee account"))
                .when(traineeService).changePassword("t", "wrong", "new");

        PasswordException e = assertThrows(PasswordException.class,
                () -> facade.changePassword(trainee("t"), new ChangePasswordRequest("t", "wrong", "new")));

        assertEquals("Password change failed: invalid username or old password", e.getMessage());
        assertEquals("Incorrect password for trainee account", e.getCause().getMessage());
        verifyNoInteractions(trainerService);
    }

    @Test
    void registerTraineeReturnsGeneratedCredentialsAndToken() {
        when(traineeService.createTrainee(any(Trainee.class))).thenAnswer(invocation -> {
            Trainee trainee = invocation.getArgument(0);
            trainee.setUsername("John.Doe");
            trainee.setPassword("$2a$hash");
            return new CreatedUser<>(trainee, "generated");
        });

        RegistrationResponse response = facade.registerTrainee(
                new TraineeRegistrationRequest("John", "Doe", LocalDate.of(2000, 1, 1), "Street"));

        ArgumentCaptor<Trainee> captor = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeService).createTrainee(captor.capture());
        assertEquals("John", captor.getValue().getFirstName());
        assertEquals("Street", captor.getValue().getAddress());
        assertEquals("John.Doe", response.getUsername());
        assertEquals("generated", response.getPassword());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1800, response.getExpiresIn());
        assertEquals(trainee("John.Doe"), JWT_SERVICE.verify(response.getToken()).user());
    }

    @Test
    void registerTrainerReturnsTrainerToken() {
        when(trainerService.createTrainer(any(Trainer.class))).thenAnswer(invocation -> {
            Trainer trainer = invocation.getArgument(0);
            trainer.setUsername("Jane.Doe");
            return new CreatedUser<>(trainer, "generated");
        });

        RegistrationResponse response = facade.registerTrainer(
                new TrainerRegistrationRequest("Jane", "Doe", TrainingType.YOGA));

        assertEquals("generated", response.getPassword());
        assertEquals(trainer("Jane.Doe"), JWT_SERVICE.verify(response.getToken()).user());
    }

    @Test
    void getTraineeProfileMapsTraineeAndTrainers() {
        Trainee trainee = new Trainee("John", "Doe", null, "Street");
        trainee.setUsername("John.Doe");
        trainee.setActive(true);
        trainee.setTrainers(Set.of(trainer("Jane.Doe", "Jane", "Doe")));
        when(traineeService.getTraineeByUsername("John.Doe")).thenReturn(Optional.of(trainee));

        TraineeProfileResponse profile = facade.getTraineeProfile(trainee("John.Doe"), "John.Doe");

        assertEquals("John", profile.getFirstName());
        assertTrue(profile.isActive());
        assertEquals(List.of(new TrainerInfo("Jane.Doe", "Jane", "Doe", TrainingType.YOGA)), profile.getTrainers());
    }

    @Test
    void getTrainerProfileMapsMissingTraineesToEmptyList() {
        Trainer trainer = trainer("Jane.Doe", "Jane", "Doe");
        when(trainerService.getTrainerByUsername("Jane.Doe")).thenReturn(Optional.of(trainer));

        TrainerProfileResponse profile = facade.getTrainerProfile(trainer("Jane.Doe"), "Jane.Doe");

        assertEquals(TrainingType.YOGA, profile.getSpecialization());
        assertEquals(List.of(), profile.getTrainees());
    }

    @Test
    void tokenOfAnotherTraineeIsForbidden() {
        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> facade.deleteTraineeProfile(trainee("Someone.Else"), "John.Doe"));

        assertEquals("Access denied: the token does not belong to the requested trainee account", e.getMessage());
        assertFalse(e.getMessage().contains("Someone.Else") || e.getMessage().contains("John.Doe"));
        verifyNoInteractions(traineeService);
    }

    @Test
    void trainerTokenCannotAccessTraineeResources() {
        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> facade.getTraineeProfile(trainer("John.Doe"), "John.Doe"));

        assertEquals("Access denied: this resource requires a trainee token, but a trainer token was provided",
                e.getMessage());
    }

    @Test
    void traineeTokenCannotAccessTrainerResources() {
        assertThrows(ForbiddenException.class,
                () -> facade.activateDeactivateTrainer(trainee("Jane.Doe"), "Jane.Doe",
                        new ActivateDeactivateRequest("Jane.Doe", false)));

        verifyNoInteractions(trainerService);
    }

    @Test
    void updateTraineeTrainersReturnsUpdatedList() {
        Trainee trainee = new Trainee();
        trainee.setTrainers(Set.of(trainer("Jane.Doe", "Jane", "Doe")));
        when(traineeService.getTraineeByUsername("John.Doe")).thenReturn(Optional.of(trainee));

        List<TrainerInfo> trainers = facade.updateTraineeTrainers(trainee("John.Doe"), "John.Doe",
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

        List<TrainingResponse> trainings = facade.getTraineeTrainings(trainee("John.Doe"), "John.Doe",
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

        List<TrainingResponse> trainings = facade.getTrainerTrainings(trainer("Jane.Doe"), "Jane.Doe",
                null, null, "John");

        assertEquals("John Doe", trainings.get(0).getTraineeName());
        assertNull(trainings.get(0).getTrainerName());
    }

    @Test
    void addTrainingRequiresTokenOfTheTrainee() {
        AddTrainingRequest request = new AddTrainingRequest("trainee", "trainer", "workout",
                LocalDate.of(2030, 1, 1), 60);

        facade.addTraining(trainee("trainee"), request);
        verify(trainingService).createTraining("trainee", "trainer", "workout",
                LocalDate.of(2030, 1, 1), Duration.ofMinutes(60));

        assertThrows(ForbiddenException.class, () -> facade.addTraining(trainer("trainee"), request));
        assertThrows(ForbiddenException.class, () -> facade.addTraining(trainee("someone-else"), request));
        verifyNoMoreInteractions(trainingService);
    }

    @Test
    void trainingTypesAreMappedWithIds() {
        when(trainingService.getTrainingTypes()).thenReturn(List.of(TrainingType.YOGA));

        List<TrainingTypeResponse> types = facade.listTrainingTypes();

        assertEquals("Yoga", types.get(0).getTrainingType());
        assertEquals((long) TrainingType.YOGA.ordinal() + 1, types.get(0).getTrainingTypeId());
    }
}
