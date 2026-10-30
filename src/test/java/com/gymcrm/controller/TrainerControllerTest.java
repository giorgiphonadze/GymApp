package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TrainerProfileResponse;
import com.gymcrm.dto.TrainerRegistrationRequest;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.UpdateTrainerRequest;
import com.gymcrm.exception.ForbiddenException;
import com.gymcrm.exception.GlobalExceptionHandler;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.facade.GymFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TrainerControllerTest {
    private static final String AUTH = "Bearer token";

    private MockMvc mockMvc;

    @Mock
    private GymFacade gymFacade;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TrainerController(gymFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testRegisterTrainer() throws Exception {
        TrainerRegistrationRequest request = new TrainerRegistrationRequest("Jane", "Doe", TrainingType.YOGA);
        when(gymFacade.registerTrainer(request)).thenReturn(new RegistrationResponse("Jane.Doe", "pass"));

        mockMvc.perform(post("/trainers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Jane.Doe"));
    }

    @Test
    void testGetTrainerProfile() throws Exception {
        TrainerProfileResponse profile = TrainerProfileResponse.builder()
                .username("Jane.Doe").specialization(TrainingType.YOGA).trainees(List.of()).build();
        when(gymFacade.getTrainerProfile(AUTH, "Jane.Doe")).thenReturn(profile);

        mockMvc.perform(get("/trainers/Jane.Doe").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Jane.Doe"));
    }

    @Test
    void testGetTrainerProfileWithExpiredTokenIsUnauthorized() throws Exception {
        when(gymFacade.getTrainerProfile(AUTH, "Jane.Doe")).thenThrow(new PasswordException("Token has expired"));

        mockMvc.perform(get("/trainers/Jane.Doe").header("Authorization", AUTH))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Token has expired"));
    }

    @Test
    void testTraineeTokenCannotAccessTrainerEndpoints() throws Exception {
        when(gymFacade.getTrainerProfile(AUTH, "Jane.Doe")).thenThrow(new ForbiddenException(
                "Access denied: this resource requires a trainer token, but a trainee token was provided"));

        mockMvc.perform(get("/trainers/Jane.Doe").header("Authorization", AUTH))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateTrainerProfile() throws Exception {
        UpdateTrainerRequest request = new UpdateTrainerRequest("Jane.Doe", "Jane", "Doe", true);
        TrainerProfileResponse profile = TrainerProfileResponse.builder()
                .username("Jane.Doe").firstName("Jane").trainees(List.of()).build();
        when(gymFacade.updateTrainerProfile(AUTH, "Jane.Doe", request)).thenReturn(profile);

        mockMvc.perform(put("/trainers/Jane.Doe")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Jane.Doe"));
    }

    @Test
    void testGetTrainerTrainings() throws Exception {
        TrainingResponse training = TrainingResponse.builder().trainingName("Morning").traineeName("John Doe").build();
        when(gymFacade.getTrainerTrainings(AUTH, "Jane.Doe", null, null, "John")).thenReturn(List.of(training));

        mockMvc.perform(get("/trainers/Jane.Doe/trainings")
                .param("traineeName", "John")
                .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].traineeName").value("John Doe"));
    }

    @Test
    void testActivateDeactivateTrainer() throws Exception {
        ActivateDeactivateRequest request = new ActivateDeactivateRequest("Jane.Doe", false);

        mockMvc.perform(patch("/trainers/Jane.Doe/active")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).activateDeactivateTrainer(AUTH, "Jane.Doe", request);
    }
}
