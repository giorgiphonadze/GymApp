package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.dto.AddTrainingRequest;
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

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TrainingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GymFacade gymFacade;

    @InjectMocks
    private TrainingController trainingController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(trainingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testAddTraining() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("trainee", "trainer", "workout", LocalDate.now(), 60);
        String auth = "Basic " + Base64.getEncoder().encodeToString("trainee:pass".getBytes());

        mockMvc.perform(post("/trainings")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).createTraining(eq("trainee"), eq("pass"), eq("trainer"), eq("workout"), any(), any());
    }

    @Test
    void testGetTrainingTypes() throws Exception {
        when(gymFacade.getTrainingTypes()).thenReturn(List.of(TrainingType.YOGA));

        mockMvc.perform(get("/training-types"))
                .andExpect(status().isOk());
    }
}
