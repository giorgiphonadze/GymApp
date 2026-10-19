package com.gymcrm.controller;

import com.gymcrm.dto.ChangePasswordRequest;
import com.gymcrm.dto.LoginRequest;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.facade.GymFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login and password management")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final GymFacade gymFacade;

    public AuthController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @Operation(summary = "Login",
            description = "Authenticates a trainee or trainer. Credentials are passed in the request body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to login: {}", request.getUsername());
        boolean authenticated = gymFacade.authenticateTrainee(request.getUsername(), request.getPassword())
                || gymFacade.authenticateTrainer(request.getUsername(), request.getPassword());
        if (!authenticated) {
            throw new PasswordException("Invalid username or password");
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Change password",
            description = "Changes the password of a trainee or trainer after verifying the old password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Invalid username or old password")
    })
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("REST request to change password: {}", request.getUsername());
        try {
            gymFacade.changeTraineePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
        } catch (PasswordException traineeFailure) {
            log.debug("User {} is not a trainee or trainee credentials did not match", request.getUsername());
            try {
                gymFacade.changeTrainerPassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
            } catch (PasswordException trainerFailure) {
                log.error("Failed to change password for user: {}", request.getUsername());
                throw new PasswordException("Invalid username or old password", trainerFailure);
            }
        }
        return ResponseEntity.ok().build();
    }
}
