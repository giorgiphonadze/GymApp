package com.gymcrm.controller;

import com.gymcrm.dto.ChangePasswordRequest;
import com.gymcrm.dto.LoginRequest;
import com.gymcrm.dto.LoginResponse;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login, logout and password management")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final GymFacade gymFacade;

    public AuthController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @Operation(summary = "Login",
            description = "Authenticates a trainee or trainer and returns a JWT. Send it on every other request "
                    + "as `Authorization: Bearer <token>`; it expires after the returned number of seconds. "
                    + "After 3 consecutive failed attempts the username is blocked for 5 minutes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful; token returned", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password"),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts; username temporarily blocked")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to login");
        return ResponseEntity.ok(gymFacade.login(request));
    }

    @Operation(summary = "Change password",
            description = "Changes the password of the authenticated trainee or trainer after verifying the old password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid token or wrong old password"),
            @ApiResponse(responseCode = "403", description = "Token does not belong to the given username")
    })
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("REST request to change password");
        gymFacade.changePassword(user, request);
        return ResponseEntity.ok().build();
    }
}
