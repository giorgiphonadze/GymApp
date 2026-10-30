package com.gymcrm.controller;

import com.gymcrm.config.OpenApiConfig;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TraineeProfileResponse;
import com.gymcrm.dto.TraineeRegistrationRequest;
import com.gymcrm.dto.TrainerInfo;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.UpdateTraineeRequest;
import com.gymcrm.facade.GymFacade;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/trainees")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Trainees", description = "Trainee profile management")
public class TraineeController {
    private static final Logger log = LoggerFactory.getLogger(TraineeController.class);
    private final GymFacade gymFacade;

    public TraineeController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @SecurityRequirements
    @Operation(summary = "Register trainee")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainee registered; username and password returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<RegistrationResponse> registerTrainee(@Valid @RequestBody TraineeRegistrationRequest request) {
        log.info("REST request to register trainee");
        return ResponseEntity.status(HttpStatus.CREATED).body(gymFacade.registerTrainee(request));
    }

    @Operation(summary = "Get trainee profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @GetMapping("/{username}")
    public ResponseEntity<TraineeProfileResponse> getTraineeProfile(
            @PathVariable("username") String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to get trainee profile: {}", username);
        return ResponseEntity.ok(gymFacade.getTraineeProfile(authHeader, username));
    }

    @Operation(summary = "Update trainee profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @PutMapping("/{username}")
    public ResponseEntity<TraineeProfileResponse> updateTraineeProfile(
            @PathVariable("username") String username,
            @Valid @RequestBody UpdateTraineeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to update trainee profile: {}", username);
        return ResponseEntity.ok(gymFacade.updateTraineeProfile(authHeader, username, request));
    }

    @Operation(summary = "Delete trainee profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee and related trainings deleted"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteTraineeProfile(
            @PathVariable("username") String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to delete trainee profile: {}", username);
        gymFacade.deleteTraineeProfile(authHeader, username);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update trainee's trainers list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated trainers list returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @PutMapping("/{username}/trainers")
    public ResponseEntity<List<TrainerInfo>> updateTraineeTrainers(
            @PathVariable("username") String username,
            @Valid @RequestBody List<String> trainerUsernames,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to update trainers for trainee: {}", username);
        return ResponseEntity.ok(gymFacade.updateTraineeTrainers(authHeader, username, trainerUsernames));
    }

    @Operation(summary = "Get trainee trainings list")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainings returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @GetMapping("/{username}/trainings")
    public ResponseEntity<List<TrainingResponse>> getTraineeTrainings(
            @PathVariable("username") String username,
            @RequestParam(value = "periodFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(value = "periodTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(value = "trainerName", required = false) String trainerName,
            @RequestParam(value = "trainingType", required = false) String trainingType,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to get trainee trainings: {}", username);
        return ResponseEntity.ok(gymFacade.getTraineeTrainings(authHeader, username, periodFrom, periodTo,
                trainerName, trainingType));
    }

    @Operation(summary = "Get active trainers not assigned to trainee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainers returned"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @GetMapping("/{username}/unassigned-trainers")
    public ResponseEntity<List<TrainerInfo>> getUnassignedTrainers(
            @PathVariable("username") String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to get unassigned trainers for trainee: {}", username);
        return ResponseEntity.ok(gymFacade.getUnassignedTrainers(authHeader, username));
    }

    @Operation(summary = "Activate/de-activate trainee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee status changed"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials"),
            @ApiResponse(responseCode = "409", description = "Trainee already in requested state")
    })
    @PatchMapping("/{username}/active")
    public ResponseEntity<Void> activateDeactivateTrainee(
            @PathVariable("username") String username,
            @Valid @RequestBody ActivateDeactivateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to change active status of trainee: {}", username);
        gymFacade.activateDeactivateTrainee(authHeader, username, request);
        return ResponseEntity.ok().build();
    }
}
