package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.TrainerRegistrationRequest;
import com.gymcrm.dto.UpdateTrainerRequest;
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
public class TrainerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GymFacade gymFacade;

    @InjectMocks
    private TrainerController trainerController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(trainerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testRegisterTrainer() throws Exception {
        TrainerRegistrationRequest request = new TrainerRegistrationRequest("Jane", "Doe", TrainingType.YOGA);
        Trainer trainer = new Trainer("Jane", "Doe", TrainingType.YOGA);
        trainer.setUsername("Jane.Doe");
        trainer.setPassword("pass");

        when(gymFacade.createTrainer(any(Trainer.class))).thenReturn(trainer);

        mockMvc.perform(post("/trainers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetTrainerProfile() throws Exception {
        Trainer trainer = new Trainer("Jane", "Doe", TrainingType.YOGA);
        trainer.setUsername("Jane.Doe");
        trainer.setTrainees(Collections.emptySet());
        String auth = "Basic " + Base64.getEncoder().encodeToString("Jane.Doe:pass".getBytes());

        when(gymFacade.getTrainer("Jane.Doe", "pass")).thenReturn(Optional.of(trainer));

        mockMvc.perform(get("/trainers/Jane.Doe")
                .header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateTrainerProfile() throws Exception {
        UpdateTrainerRequest request = new UpdateTrainerRequest("Jane.Doe", "Jane", "Doe", true);
        Trainer existing = new Trainer("Jane", "Doe", TrainingType.YOGA);
        existing.setUsername("Jane.Doe");
        existing.setUserId(2L);
        existing.setPassword("pass");
        existing.setTrainees(Collections.emptySet());
        String auth = "Basic " + Base64.getEncoder().encodeToString("Jane.Doe:pass".getBytes());

        when(gymFacade.getTrainer("Jane.Doe", "pass")).thenReturn(Optional.of(existing));

        mockMvc.perform(put("/trainers/Jane.Doe")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }


    @Test
    void testGetTrainerProfileNotFound() throws Exception {
        String auth = "Basic " + Base64.getEncoder().encodeToString("nonexistent:pass".getBytes());
        when(gymFacade.getTrainer("nonexistent", "pass")).thenReturn(Optional.empty());

        mockMvc.perform(get("/trainers/nonexistent")
                .header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetTrainerTrainings() throws Exception {
        String auth = "Basic " + Base64.getEncoder().encodeToString("Jane.Doe:pass".getBytes());
        when(gymFacade.getTrainerTrainings(eq("Jane.Doe"), eq("pass"), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/trainers/Jane.Doe/trainings")
                .header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void testActivateDeactivateTrainer() throws Exception {
        ActivateDeactivateRequest request = new ActivateDeactivateRequest("Jane.Doe", false);
        String auth = "Basic " + Base64.getEncoder().encodeToString("Jane.Doe:pass".getBytes());

        mockMvc.perform(patch("/trainers/Jane.Doe/active")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).activateDeactivateTrainer("Jane.Doe", "pass", false);
    }
}
