package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.domain.UserType;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TraineeProfileResponse;
import com.gymcrm.dto.TraineeRegistrationRequest;
import com.gymcrm.dto.TrainerInfo;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.UpdateTraineeRequest;
import com.gymcrm.exception.ForbiddenException;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TraineeControllerTest {

    private MockMvc mockMvc;
    private AuthenticatedUser user;

    @Mock
    private GymFacade gymFacade;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new TraineeController(gymFacade));
        user = ControllerTestSupport.authenticateAs("John.Doe", UserType.TRAINEE);
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void testRegisterTrainee() throws Exception {
        TraineeRegistrationRequest request = new TraineeRegistrationRequest("John", "Doe", null, "Address");
        when(gymFacade.registerTrainee(request))
                .thenReturn(new RegistrationResponse("John.Doe", "pass", "jwt", "Bearer", 1800));

        mockMvc.perform(post("/trainees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("John.Doe"))
                .andExpect(jsonPath("$.password").value("pass"))
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void testRegisterTraineeInvalid() throws Exception {
        TraineeRegistrationRequest request = new TraineeRegistrationRequest("", "", null, "Address");

        mockMvc.perform(post("/trainees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testGetTraineeProfile() throws Exception {
        TraineeProfileResponse profile = TraineeProfileResponse.builder()
                .username("John.Doe").firstName("John").lastName("Doe").trainers(List.of()).build();
        when(gymFacade.getTraineeProfile(user, "John.Doe")).thenReturn(profile);

        mockMvc.perform(get("/trainees/John.Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void testGetProfileOfDeletedAccountIsUnauthorized() throws Exception {
        when(gymFacade.getTraineeProfile(user, "John.Doe"))
                .thenThrow(new PasswordException("Trainee account not found"));

        mockMvc.perform(get("/trainees/John.Doe"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Trainee account not found"));
    }

    @Test
    void testGetOtherTraineeProfileIsForbidden() throws Exception {
        when(gymFacade.getTraineeProfile(user, "Someone.Else"))
                .thenThrow(new ForbiddenException("Access denied: the token does not belong to the requested trainee account"));

        mockMvc.perform(get("/trainees/Someone.Else"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Access denied: the token does not belong to the requested trainee account"));
    }

    @Test
    void testUpdateTraineeProfile() throws Exception {
        UpdateTraineeRequest request = new UpdateTraineeRequest("John.Doe", "John", "Doe", null, "New Address", true);
        TraineeProfileResponse profile = TraineeProfileResponse.builder()
                .username("John.Doe").address("New Address").trainers(List.of()).build();
        when(gymFacade.updateTraineeProfile(user, "John.Doe", request)).thenReturn(profile);

        mockMvc.perform(put("/trainees/John.Doe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John.Doe"))
                .andExpect(jsonPath("$.address").value("New Address"));
    }

    @Test
    void testDeleteTraineeProfile() throws Exception {
        mockMvc.perform(delete("/trainees/John.Doe"))
                .andExpect(status().isOk());

        verify(gymFacade).deleteTraineeProfile(user, "John.Doe");
    }

    @Test
    void testActivateDeactivateTrainee() throws Exception {
        ActivateDeactivateRequest request = new ActivateDeactivateRequest("John.Doe", false);

        mockMvc.perform(patch("/trainees/John.Doe/active")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).activateDeactivateTrainee(user, "John.Doe", request);
    }

    @Test
    void testActivateAlreadyActiveTraineeIsConflict() throws Exception {
        ActivateDeactivateRequest request = new ActivateDeactivateRequest("John.Doe", true);
        doThrow(new IllegalStateException("Trainee is already active"))
                .when(gymFacade).activateDeactivateTrainee(user, "John.Doe", request);

        mockMvc.perform(patch("/trainees/John.Doe/active")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateTraineeTrainers() throws Exception {
        List<String> trainerUsernames = List.of("trainer1");
        when(gymFacade.updateTraineeTrainers(user, "John.Doe", trainerUsernames))
                .thenReturn(List.of(new TrainerInfo("trainer1", "Jane", "Doe", TrainingType.YOGA)));

        mockMvc.perform(put("/trainees/John.Doe/trainers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trainerUsernames)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("trainer1"));
    }

    @Test
    void testGetTraineeTrainings() throws Exception {
        TrainingResponse training = TrainingResponse.builder().trainingName("Morning").trainerName("Jane Doe").build();
        when(gymFacade.getTraineeTrainings(user, "John.Doe", LocalDate.of(2030, 1, 1), null, "Jane", null))
                .thenReturn(List.of(training));

        mockMvc.perform(get("/trainees/John.Doe/trainings")
                .param("periodFrom", "2030-01-01")
                .param("trainerName", "Jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Morning"));
    }

    @Test
    void testGetUnassignedTrainers() throws Exception {
        when(gymFacade.getUnassignedTrainers(user, "John.Doe")).thenReturn(List.of());

        mockMvc.perform(get("/trainees/John.Doe/unassigned-trainers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testUnexpectedFailureDoesNotLeakDetails() throws Exception {
        when(gymFacade.getUnassignedTrainers(any(), any())).thenThrow(new RuntimeException("db password=secret"));

        mockMvc.perform(get("/trainees/John.Doe/unassigned-trainers"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred"));
    }
}
