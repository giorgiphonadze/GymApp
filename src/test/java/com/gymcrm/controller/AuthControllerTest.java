package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.domain.UserType;
import com.gymcrm.dto.ChangePasswordRequest;
import com.gymcrm.dto.LoginRequest;
import com.gymcrm.exception.GlobalExceptionHandler;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GymFacade gymFacade;

    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authController = new AuthController(gymFacade, TestTokens.JWT_SERVICE);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testLoginSuccess() throws Exception {
        when(gymFacade.login("user", "pass")).thenReturn(Optional.of(UserType.TRAINEE));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user", "pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void testLoginReturnsTokenIssuedForTheAuthenticatedUser() throws Exception {
        when(gymFacade.login("coach", "pass")).thenReturn(Optional.of(UserType.TRAINER));

        String body = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("coach", "pass"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("token").asText();
        AuthenticatedUser user = TestTokens.JWT_SERVICE.authenticate("Bearer " + token);
        assertEquals("coach", user.username());
        assertEquals(UserType.TRAINER, user.userType());
    }

    @Test
    void testLoginFailure() throws Exception {
        when(gymFacade.login("user", "pass")).thenReturn(Optional.empty());

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
