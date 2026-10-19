package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.dto.ChangePasswordRequest;
import com.gymcrm.dto.LoginRequest;
import com.gymcrm.dto.LoginRequest;
import com.gymcrm.exception.GlobalExceptionHandler;
import com.gymcrm.exception.PasswordException;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GymFacade gymFacade;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testLoginSuccess() throws Exception {
        when(gymFacade.authenticateTrainee("user", "pass")).thenReturn(true);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user", "pass"))))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginFailure() throws Exception {
        when(gymFacade.authenticateTrainee("user", "pass")).thenReturn(false);
        when(gymFacade.authenticateTrainer("user", "pass")).thenReturn(false);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user", "pass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testChangePasswordSuccess() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("user", "old", "new");

        mockMvc.perform(put("/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).changeTraineePassword("user", "old", "new");
    }

    @Test
    void testLoginMissingPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginWithGetIsNotAllowed() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testChangePasswordFallsBackToTrainer() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("trainer", "old", "new");
        doThrow(new PasswordException("Authentication failed"))
                .when(gymFacade).changeTraineePassword("trainer", "old", "new");

        mockMvc.perform(put("/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).changeTrainerPassword("trainer", "old", "new");
    }

    @Test
    void testChangePasswordWrongCredentialsReturnsUnauthorized() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("user", "wrong", "new");
        doThrow(new PasswordException("Authentication failed"))
                .when(gymFacade).changeTraineePassword("user", "wrong", "new");
        doThrow(new PasswordException("Authentication failed"))
                .when(gymFacade).changeTrainerPassword("user", "wrong", "new");

        mockMvc.perform(put("/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
