package com.gymcrm.controller;

import com.gymcrm.config.OpenApiConfig;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TrainerProfileResponse;
import com.gymcrm.dto.TrainerRegistrationRequest;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.UpdateTrainerRequest;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/trainers")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Trainers", description = "Trainer profile management")
public class TrainerController {
    private static final Logger log = LoggerFactory.getLogger(TrainerController.class);
    private final GymFacade gymFacade;

    public TrainerController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @SecurityRequirements
    @Operation(summary = "Register trainer",
            description = "Public endpoint. Generates the username and password and returns them together with a JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainer registered; username, password and token returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<RegistrationResponse> registerTrainer(@Valid @RequestBody TrainerRegistrationRequest request) {
        log.info("REST request to register trainer");
        return ResponseEntity.status(HttpStatus.CREATED).body(gymFacade.registerTrainer(request));
    }

    @Operation(summary = "Get trainer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @GetMapping("/{username}")
    public ResponseEntity<TrainerProfileResponse> getTrainerProfile(
            @PathVariable("username") String username,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("REST request to get trainer profile: {}", username);
        return ResponseEntity.ok(gymFacade.getTrainerProfile(user, username));
    }

    @Operation(summary = "Update trainer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @PutMapping("/{username}")
    public ResponseEntity<TrainerProfileResponse> updateTrainerProfile(
            @PathVariable("username") String username,
            @Valid @RequestBody UpdateTrainerRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("REST request to update trainer profile: {}", username);
        return ResponseEntity.ok(gymFacade.updateTrainerProfile(user, username, request));
    }

    @Operation(summary = "Get trainer trainings list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainings returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @GetMapping("/{username}/trainings")
    public ResponseEntity<List<TrainingResponse>> getTrainerTrainings(
            @PathVariable("username") String username,
            @RequestParam(value = "periodFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(value = "periodTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(value = "traineeName", required = false) String traineeName,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("REST request to get trainer trainings: {}", username);
        return ResponseEntity.ok(gymFacade.getTrainerTrainings(user, username, periodFrom, periodTo, traineeName));
    }

    @Operation(summary = "Activate/de-activate trainer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer status changed"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials"),
            @ApiResponse(responseCode = "409", description = "Trainer already in requested state")
    })
    @PatchMapping("/{username}/active")
    public ResponseEntity<Void> activateDeactivateTrainer(
            @PathVariable("username") String username,
            @Valid @RequestBody ActivateDeactivateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("REST request to change active status of trainer: {}", username);
        gymFacade.activateDeactivateTrainer(user, username, request);
        return ResponseEntity.ok().build();
    }
}
