package com.gymcrm.controller;

import com.gymcrm.dto.ChangePasswordRequest;
import com.gymcrm.facade.GymFacade;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final GymFacade gymFacade;

    public AuthController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        log.info("REST request to login: {}", username);
        boolean authenticated = gymFacade.authenticateTrainee(username, password) || 
                                gymFacade.authenticateTrainer(username, password);
        if (authenticated) {
            return ResponseEntity.ok().build();
        } else {
            throw new SecurityException("Authentication failed");
        }
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("REST request to change password: {}", request.getUsername());
        boolean success = false;
        try {
            gymFacade.changeTraineePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
            success = true;
        } catch (Exception e) {
             log.debug("User {} is not a trainee or trainee password change failed", request.getUsername());
        }
        
        if (!success) {
            try {
                gymFacade.changeTrainerPassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
                success = true;
            } catch (Exception e) {
                log.error("Failed to change password for user: {}", request.getUsername());
                throw new SecurityException("Authentication failed or user not found");
            }
        }
        return ResponseEntity.ok().build();
    }
}
