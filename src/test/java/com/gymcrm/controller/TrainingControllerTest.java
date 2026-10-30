package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.dto.AddTrainingRequest;
import com.gymcrm.dto.TrainingTypeResponse;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TrainingControllerTest {
    private static final String AUTH = "Bearer token";

    private MockMvc mockMvc;

    @Mock
    private GymFacade gymFacade;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TrainingController(gymFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testAddTraining() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("trainee", "trainer", "workout", LocalDate.now(), 60);

        mockMvc.perform(post("/trainings")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).addTraining(AUTH, request);
    }

    @Test
    void testAddTrainingForAnotherTraineeIsForbidden() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("someone-else", "trainer", "workout", LocalDate.now(), 60);
        doThrow(new ForbiddenException("Access denied: the token does not belong to the requested trainee account"))
                .when(gymFacade).addTraining(AUTH, request);

        mockMvc.perform(post("/trainings")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddTrainingWithInvalidBodyIsBadRequest() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("", "trainer", "workout", LocalDate.now(), 60);

        mockMvc.perform(post("/trainings")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testGetTrainingTypes() throws Exception {
        when(gymFacade.getTrainingTypes(AUTH)).thenReturn(List.of(new TrainingTypeResponse("Yoga", 1L)));

        mockMvc.perform(get("/training-types").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingType").value("Yoga"));
    }

    @Test
    void testGetTrainingTypesWithoutTokenIsUnauthorized() throws Exception {
        when(gymFacade.getTrainingTypes((String) null))
                .thenThrow(new PasswordException("Authorization header is missing"));

        mockMvc.perform(get("/training-types"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Authorization header is missing"));
    }
}
