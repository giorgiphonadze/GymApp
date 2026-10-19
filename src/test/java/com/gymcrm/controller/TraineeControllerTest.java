package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.domain.Trainee;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.TraineeRegistrationRequest;
import com.gymcrm.dto.UpdateTraineeRequest;
import com.gymcrm.exception.GlobalExceptionHandler;
import com.gymcrm.facade.GymFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TraineeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GymFacade gymFacade;

    private TraineeController traineeController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        traineeController = new TraineeController(gymFacade, TestTokens.JWT_SERVICE);
        mockMvc = MockMvcBuilders.standaloneSetup(traineeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testRegisterTrainee() throws Exception {
        TraineeRegistrationRequest request = new TraineeRegistrationRequest("John", "Doe", null, "Address");
        Trainee trainee = new Trainee("John", "Doe", null, "Address");
        trainee.setUsername("John.Doe");
        trainee.setPassword("pass");

        when(gymFacade.createTrainee(any(Trainee.class))).thenReturn(trainee);

        mockMvc.perform(post("/trainees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testRegisterTraineeInvalid() throws Exception {
        TraineeRegistrationRequest request = new TraineeRegistrationRequest("", "", null, "Address");

        mockMvc.perform(post("/trainees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetTraineeProfile() throws Exception {
        Trainee trainee = new Trainee("John", "Doe", null, "Address");
        trainee.setUsername("John.Doe");
        trainee.setTrainers(Collections.emptySet());
        String auth = TestTokens.trainee("John.Doe");

        when(gymFacade.getTrainee("John.Doe")).thenReturn(Optional.of(trainee));

        mockMvc.perform(get("/trainees/John.Doe")
                .header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTraineeProfileWithoutCredentialsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/trainees/John.Doe"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testGetTraineeProfileWithInvalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/trainees/John.Doe").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testUpdateTraineeProfile() throws Exception {
        UpdateTraineeRequest request = new UpdateTraineeRequest("John.Doe", "John", "Doe", null, "New Address", true);
        Trainee existing = new Trainee("John", "Doe", null, "Address");
        existing.setUsername("John.Doe");
        existing.setUserId(1L);
        existing.setTrainers(Collections.emptySet());
        existing.setPassword("pass");
        String auth = TestTokens.trainee("John.Doe");

        when(gymFacade.updateTrainee("John.Doe", "John", "Doe", null, "New Address", true)).thenReturn(existing);

        mockMvc.perform(put("/trainees/John.Doe")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John.Doe"));
    }

    @Test
    void testUpdateOtherTraineeProfileIsForbidden() throws Exception {
        UpdateTraineeRequest request = new UpdateTraineeRequest("John.Doe", "John", "Doe", null, "New Address", true);

        mockMvc.perform(put("/trainees/John.Doe")
                .header("Authorization", TestTokens.trainee("Someone.Else"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testDeleteTraineeProfile() throws Exception {
        String auth = TestTokens.trainee("John.Doe");

        mockMvc.perform(delete("/trainees/John.Doe")
                .header("Authorization", auth))
                .andExpect(status().isOk());

        verify(gymFacade).deleteTrainee("John.Doe");
    }

    @Test
    void testActivateDeactivateTrainee() throws Exception {
        ActivateDeactivateRequest request = new ActivateDeactivateRequest("John.Doe", false);
        String auth = TestTokens.trainee("John.Doe");

        mockMvc.perform(patch("/trainees/John.Doe/active")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).activateDeactivateTrainee("John.Doe", false);
    }

    @Test
    void testUpdateTraineeTrainers() throws Exception {
        String auth = TestTokens.trainee("John.Doe");
        Trainee trainee = new Trainee();
        trainee.setTrainers(Collections.emptySet());

        when(gymFacade.getTrainee("John.Doe")).thenReturn(Optional.of(trainee));

        mockMvc.perform(put("/trainees/John.Doe/trainers")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonList("trainer1"))))
                .andExpect(status().isOk());

        verify(gymFacade).updateTraineeTrainers(eq("John.Doe"), anyList());
    }

    @Test
    void testGetTraineeTrainings() throws Exception {
        String auth = TestTokens.trainee("John.Doe");
        when(gymFacade.getTraineeTrainings(eq("John.Doe"), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trainees/John.Doe/trainings")
                .header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUnassignedTrainers() throws Exception {
        String auth = TestTokens.trainee("John.Doe");
        when(gymFacade.getTrainersNotAssignedToTrainee("John.Doe")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trainees/John.Doe/unassigned-trainers")
                .header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void testGetOtherTraineeProfileIsForbidden() throws Exception {
        mockMvc.perform(get("/trainees/Someone.Else").header("Authorization", TestTokens.trainee("John.Doe")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testTrainerTokenCannotAccessTraineeEndpoints() throws Exception {
        mockMvc.perform(get("/trainees/John.Doe").header("Authorization", TestTokens.trainer("John.Doe")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testBasicCredentialsAreNoLongerAccepted() throws Exception {
        mockMvc.perform(get("/trainees/John.Doe").header("Authorization", "Basic Sm9obi5Eb2U6cGFzcw=="))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(gymFacade);
    }
}