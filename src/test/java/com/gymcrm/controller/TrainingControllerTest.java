package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymcrm.domain.UserType;
import com.gymcrm.dto.AddTrainingRequest;
import com.gymcrm.dto.TrainingTypeResponse;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TrainingControllerTest {

    private MockMvc mockMvc;
    private AuthenticatedUser user;

    @Mock
    private GymFacade gymFacade;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new TrainingController(gymFacade));
        user = ControllerTestSupport.authenticateAs("trainee", UserType.TRAINEE);
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void testAddTraining() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("trainee", "trainer", "workout", LocalDate.now(), 60);

        mockMvc.perform(post("/trainings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).addTraining(user, request);
    }

    @Test
    void testAddTrainingForAnotherTraineeIsForbidden() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("someone-else", "trainer", "workout", LocalDate.now(), 60);
        doThrow(new ForbiddenException("Access denied: the token does not belong to the requested trainee account"))
                .when(gymFacade).addTraining(user, request);

        mockMvc.perform(post("/trainings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddTrainingWithInvalidBodyIsBadRequest() throws Exception {
        AddTrainingRequest request = new AddTrainingRequest("", "trainer", "workout", LocalDate.now(), 60);

        mockMvc.perform(post("/trainings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testGetTrainingTypes() throws Exception {
        when(gymFacade.listTrainingTypes()).thenReturn(List.of(new TrainingTypeResponse("Yoga", 1L)));

        mockMvc.perform(get("/training-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingType").value("Yoga"));
    }
}
