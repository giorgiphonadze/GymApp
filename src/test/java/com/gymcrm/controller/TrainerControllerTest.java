package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.domain.UserType;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TrainerProfileResponse;
import com.gymcrm.dto.TrainerRegistrationRequest;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.UpdateTrainerRequest;
import com.gymcrm.exception.ForbiddenException;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TrainerControllerTest {

    private MockMvc mockMvc;
    private AuthenticatedUser user;

    @Mock
    private GymFacade gymFacade;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new TrainerController(gymFacade));
        user = ControllerTestSupport.authenticateAs("Jane.Doe", UserType.TRAINER);
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void testRegisterTrainer() throws Exception {
        TrainerRegistrationRequest request = new TrainerRegistrationRequest("Jane", "Doe", TrainingType.YOGA);
        when(gymFacade.registerTrainer(request))
                .thenReturn(new RegistrationResponse("Jane.Doe", "pass", "jwt", "Bearer", 1800));

        mockMvc.perform(post("/trainers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Jane.Doe"))
                .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    void testGetTrainerProfile() throws Exception {
        TrainerProfileResponse profile = TrainerProfileResponse.builder()
                .username("Jane.Doe").specialization(TrainingType.YOGA).trainees(List.of()).build();
        when(gymFacade.getTrainerProfile(user, "Jane.Doe")).thenReturn(profile);

        mockMvc.perform(get("/trainers/Jane.Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Jane.Doe"));
    }

    @Test
    void testTraineeTokenCannotAccessTrainerEndpoints() throws Exception {
        when(gymFacade.getTrainerProfile(user, "Jane.Doe")).thenThrow(new ForbiddenException(
                "Access denied: this resource requires a trainer token, but a trainee token was provided"));

        mockMvc.perform(get("/trainers/Jane.Doe"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateTrainerProfile() throws Exception {
        UpdateTrainerRequest request = new UpdateTrainerRequest("Jane.Doe", "Jane", "Doe", true);
        TrainerProfileResponse profile = TrainerProfileResponse.builder()
                .username("Jane.Doe").firstName("Jane").trainees(List.of()).build();
        when(gymFacade.updateTrainerProfile(user, "Jane.Doe", request)).thenReturn(profile);

        mockMvc.perform(put("/trainers/Jane.Doe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Jane.Doe"));
    }

    @Test
    void testGetTrainerTrainings() throws Exception {
        TrainingResponse training = TrainingResponse.builder().trainingName("Morning").traineeName("John Doe").build();
        when(gymFacade.getTrainerTrainings(user, "Jane.Doe", null, null, "John")).thenReturn(List.of(training));

        mockMvc.perform(get("/trainers/Jane.Doe/trainings")
                .param("traineeName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].traineeName").value("John Doe"));
    }

    @Test
    void testActivateDeactivateTrainer() throws Exception {
        ActivateDeactivateRequest request = new ActivateDeactivateRequest("Jane.Doe", false);

        mockMvc.perform(patch("/trainers/Jane.Doe/active")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).activateDeactivateTrainer(user, "Jane.Doe", request);
    }
}
