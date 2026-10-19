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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Base64;
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

    @InjectMocks
    private TraineeController traineeController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
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
        // verify response body manually if needed
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
        String auth = "Basic " + Base64.getEncoder().encodeToString("John.Doe:pass".getBytes());

        when(gymFacade.getTrainee("John.Doe", "pass")).thenReturn(Optional.of(trainee));

        mockMvc.perform(get("/trainees/John.Doe")
                .header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateTraineeProfile() throws Exception {
        UpdateTraineeRequest request = new UpdateTraineeRequest("John.Doe", "John", "Doe", null, "New Address", true);
        Trainee existing = new Trainee("John", "Doe", null, "Address");
        existing.setUsername("John.Doe");
        existing.setUserId(1L);
        existing.setTrainers(Collections.emptySet());
        existing.setPassword("pass");
        String auth = "Basic " + Base64.getEncoder().encodeToString("John.Doe:pass".getBytes());

        when(gymFacade.getTrainee("John.Doe", "pass")).thenReturn(Optional.of(existing));

        mockMvc.perform(put("/trainees/John.Doe")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteTraineeProfile() throws Exception {
        String auth = "Basic " + Base64.getEncoder().encodeToString("John.Doe:pass".getBytes());

        mockMvc.perform(delete("/trainees/John.Doe")
                .header("Authorization", auth))
                .andExpect(status().isOk());

        verify(gymFacade).deleteTrainee("John.Doe", "pass");
    }

    @Test
    void testActivateDeactivateTrainee() throws Exception {
        ActivateDeactivateRequest request = new ActivateDeactivateRequest("John.Doe", false);
        String auth = "Basic " + Base64.getEncoder().encodeToString("John.Doe:pass".getBytes());

        mockMvc.perform(patch("/trainees/John.Doe/active")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).activateDeactivateTrainee("John.Doe", "pass", false);
    }

    @Test
    void testUpdateTraineeTrainers() throws Exception {
        String auth = "Basic " + Base64.getEncoder().encodeToString("John.Doe:pass".getBytes());
        Trainee trainee = new Trainee();
        trainee.setTrainers(Collections.emptySet());

        when(gymFacade.getTrainee("John.Doe", "pass")).thenReturn(Optional.of(trainee));

        mockMvc.perform(put("/trainees/John.Doe/trainers")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonList("trainer1"))))
                .andExpect(status().isOk());

        verify(gymFacade).updateTraineeTrainers(eq("John.Doe"), eq("pass"), anyList());
    }

    @Test
    void testGetTraineeTrainings() throws Exception {
        String auth = "Basic " + Base64.getEncoder().encodeToString("John.Doe:pass".getBytes());
        when(gymFacade.getTraineeTrainings(eq("John.Doe"), eq("pass"), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trainees/John.Doe/trainings")
                .header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUnassignedTrainers() throws Exception {
        String auth = "Basic " + Base64.getEncoder().encodeToString("John.Doe:pass".getBytes());
        when(gymFacade.getTrainersNotAssignedToTrainee("John.Doe", "pass")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trainees/John.Doe/unassigned-trainers")
                .header("Authorization", auth))
                .andExpect(status().isOk());
    }
}
