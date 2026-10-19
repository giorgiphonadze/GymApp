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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TrainingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GymFacade gymFacade;

    private TrainingController trainingController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        trainingController = new TrainingController(gymFacade, TestTokens.JWT_SERVICE);
        mockMvc = MockMvcBuilders.standaloneSetup(trainingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testAddTraining() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("trainee", "trainer", "workout", LocalDate.now(), 60);
        String auth = TestTokens.trainee("trainee");

        mockMvc.perform(post("/trainings")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).createTraining(eq("trainee"), eq("trainer"), eq("workout"), any(), any());
    }

    @Test
    void testAddTrainingForAnotherTraineeIsForbidden() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("someone-else", "trainer", "workout", LocalDate.now(), 60);

        mockMvc.perform(post("/trainings")
                .header("Authorization", TestTokens.trainee("trainee"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testAddTrainingWithTrainerTokenIsForbidden() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("trainee", "trainer", "workout", LocalDate.now(), 60);

        mockMvc.perform(post("/trainings")
                .header("Authorization", TestTokens.trainer("trainee"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testAddTrainingWithoutTokenIsUnauthorized() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("trainee", "trainer", "workout", LocalDate.now(), 60);

        mockMvc.perform(post("/trainings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testGetTrainingTypesAcceptsTraineeAndTrainerTokens() throws Exception {
        when(gymFacade.getTrainingTypes()).thenReturn(List.of(TrainingType.YOGA));

        mockMvc.perform(get("/training-types").header("Authorization", TestTokens.trainee("trainee")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingType").value("Yoga"));
        mockMvc.perform(get("/training-types").header("Authorization", TestTokens.trainer("trainer")))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTrainingTypesWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/training-types"))
                .andExpect(status().isUnauthorized());

        verify(gymFacade, never()).getTrainingTypes();
    }

    @Test
    void testGetTrainingTypesWithInvalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/training-types").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());

        verify(gymFacade, never()).getTrainingTypes();
    }

    @Test
    void testGetTrainingTypesWithBasicCredentialsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/training-types").header("Authorization", "Basic dHJhaW5lZTpwYXNz"))
                .andExpect(status().isUnauthorized());

        verify(gymFacade, never()).getTrainingTypes();
    }
}