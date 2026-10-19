package com.gymcrm.controller;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.UserType;
import com.gymcrm.config.OpenApiConfig;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TraineeProfileResponse;
import com.gymcrm.dto.TraineeRegistrationRequest;
import com.gymcrm.dto.TrainerInfo;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.UpdateTraineeRequest;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.security.JwtService;
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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/trainees")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Trainees", description = "Trainee profile management")
public class TraineeController {
    private static final Logger log = LoggerFactory.getLogger(TraineeController.class);
    private final GymFacade gymFacade;
    private final JwtService jwtService;

    public TraineeController(GymFacade gymFacade, JwtService jwtService) {
        this.gymFacade = gymFacade;
        this.jwtService = jwtService;
    }

    @SecurityRequirements
    @Operation(summary = "Register trainee")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainee registered; username and password returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<RegistrationResponse> registerTrainee(@Valid @RequestBody TraineeRegistrationRequest request) {
        log.info("REST request to register trainee: {} {}", request.getFirstName(), request.getLastName());
        Trainee trainee = new Trainee(request.getFirstName(), request.getLastName(),
                request.getDateOfBirth(), request.getAddress());
        Trainee created = gymFacade.createTrainee(trainee);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistrationResponse(created.getUsername(), created.getPassword()));
    }

    @Operation(summary = "Get trainee profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainee profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @GetMapping("/{username}")
    public ResponseEntity<TraineeProfileResponse> getTraineeProfile(
            @PathVariable("username") String username,
            @RequestHeader(value = "Authorization", required = false)
            String authHeader) {

        log.info("REST request to get trainee profile: {}", username);
        authorize(authHeader, username);
        return gymFacade.getTrainee(username)
                .map(this::mapToTraineeProfileResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
        authorize(authHeader, username);
        Trainee updated = gymFacade.updateTrainee(username, request.getFirstName(), request.getLastName(),
                request.getDateOfBirth(), request.getAddress(), request.getIsActive());
        return ResponseEntity.ok(mapToTraineeProfileResponse(updated));
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
        authorize(authHeader, username);
        gymFacade.deleteTrainee(username);
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
        authorize(authHeader, username);
        gymFacade.updateTraineeTrainers(username, trainerUsernames);

        Trainee updated = gymFacade.getTrainee(username).get();
        return ResponseEntity.ok(updated.getTrainers().stream()
                .map(trainer -> new TrainerInfo(trainer.getUsername(), trainer.getFirstName(),
                        trainer.getLastName(), trainer.getSpecialization()))
                .collect(Collectors.toList()));
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
        authorize(authHeader, username);
        List<Training> trainings = gymFacade.getTraineeTrainings(username, periodFrom, periodTo, trainerName, trainingType);
        return ResponseEntity.ok(trainings.stream()
                .map(training -> TrainingResponse.builder()
                        .trainingName(training.getTrainingName())
                        .trainingDate(training.getTrainingDate())
                        .trainingType(training.getTrainingType())
                        .trainingDuration((int) training.getTrainingDuration().toMinutes())
                        .trainerName(training.getTrainer().getFirstName() + " " + training.getTrainer().getLastName())
                        .build())
                .collect(Collectors.toList()));
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
        authorize(authHeader, username);
        Collection<Trainer> trainers = gymFacade.getTrainersNotAssignedToTrainee(username);
        return ResponseEntity.ok(trainers.stream()
                .map(trainer -> new TrainerInfo(trainer.getUsername(), trainer.getFirstName(),
                        trainer.getLastName(), trainer.getSpecialization()))
                .collect(Collectors.toList()));
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

        log.info("REST request to {} trainee: {}", request.getIsActive() ? "activate" : "deactivate", username);
        authorize(authHeader, username);
        gymFacade.activateDeactivateTrainee(username, request.getIsActive());
        return ResponseEntity.ok().build();
    }

    private TraineeProfileResponse mapToTraineeProfileResponse(Trainee trainee) {
        return TraineeProfileResponse.builder()
                .username(trainee.getUsername())
                .firstName(trainee.getFirstName())
                .lastName(trainee.getLastName())
                .dateOfBirth(trainee.getDateOfBirth())
                .address(trainee.getAddress())
                .isActive(trainee.isActive())
                .trainers(trainee.getTrainers().stream()
                        .map(trainer -> new TrainerInfo(trainer.getUsername(), trainer.getFirstName(),
                                trainer.getLastName(), trainer.getSpecialization()))
                        .collect(Collectors.toList()))
                .build();
    }

    private void authorize(String authHeader, String username) {
        jwtService.authenticate(authHeader).requireSelf(UserType.TRAINEE, username);
    }
}
