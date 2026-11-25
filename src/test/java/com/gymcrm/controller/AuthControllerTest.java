package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.domain.UserType;
import com.gymcrm.dto.ChangePasswordRequest;
import com.gymcrm.dto.LoginRequest;
import com.gymcrm.dto.LoginResponse;
import com.gymcrm.exception.ForbiddenException;
import com.gymcrm.exception.LoginBlockedException;
import com.gymcrm.exception.PasswordException;
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

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GymFacade gymFacade;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new AuthController(gymFacade));
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void testLoginReturnsFacadeResponse() throws Exception {
        LoginRequest request = new LoginRequest("user", "pass");
        when(gymFacade.login(request)).thenReturn(new LoginResponse("jwt", "Bearer", 1800));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void testLoginFailure() throws Exception {
        when(gymFacade.login(any(LoginRequest.class)))
                .thenThrow(new PasswordException("Login failed: invalid username or password"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user", "pass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Login failed: invalid username or password"));
    }

    @Test
    void testBlockedLoginReturnsTooManyRequestsWithRetryAfter() throws Exception {
        when(gymFacade.login(any(LoginRequest.class))).thenThrow(new LoginBlockedException(
                "Too many failed login attempts. Try again in 5 minute(s)", Duration.ofMinutes(5)));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user", "pass"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(content().string("Too many failed login attempts. Try again in 5 minute(s)"));
    }

    @Test
    void testLoginMissingPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user", ""))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(gymFacade);
    }

    @Test
    void testLoginWithGetIsNotAllowed() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testChangePasswordDelegatesToFacadeWithAuthenticatedUser() throws Exception {
        AuthenticatedUser user = ControllerTestSupport.authenticateAs("user", UserType.TRAINEE);
        ChangePasswordRequest request = new ChangePasswordRequest("user", "old", "new");

        mockMvc.perform(put("/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gymFacade).changePassword(user, request);
    }

    @Test
    void testChangePasswordWrongCredentialsReturnsUnauthorized() throws Exception {
        ControllerTestSupport.authenticateAs("user", UserType.TRAINEE);
        doThrow(new PasswordException("Password change failed: invalid username or old password"))
                .when(gymFacade).changePassword(any(), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangePasswordRequest("user", "wrong", "new"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Password change failed: invalid username or old password"));
    }

    @Test
    void testChangePasswordOfAnotherUserIsForbidden() throws Exception {
        AuthenticatedUser user = ControllerTestSupport.authenticateAs("user", UserType.TRAINEE);
        doThrow(new ForbiddenException("Access denied: the token does not belong to the requested trainee account"))
                .when(gymFacade).changePassword(eq(user), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangePasswordRequest("other", "old", "new"))))
                .andExpect(status().isForbidden());
    }
}
