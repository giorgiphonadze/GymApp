package com.gymcrm.controller;

import com.gymcrm.config.OpenApiConfig;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.UserType;
import com.gymcrm.domain.Training;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TraineeInfo;
import com.gymcrm.dto.TrainerProfileResponse;
import com.gymcrm.dto.TrainerRegistrationRequest;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.UpdateTrainerRequest;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/trainers")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Trainers", description = "Trainer profile management")
public class TrainerController {
    private static final Logger log = LoggerFactory.getLogger(TrainerController.class);
    private final GymFacade gymFacade;
    private final JwtService jwtService;

    public TrainerController(GymFacade gymFacade, JwtService jwtService) {
        this.gymFacade = gymFacade;
        this.jwtService = jwtService;
    }

    @SecurityRequirements
    @Operation(summary = "Register trainer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trainer registered; username and password returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<RegistrationResponse> registerTrainer(@Valid @RequestBody TrainerRegistrationRequest request) {
        log.info("REST request to register trainer: {} {}", request.getFirstName(), request.getLastName());
        Trainer trainer = new Trainer(request.getFirstName(), request.getLastName(), request.getSpecialization());
        Trainer created = gymFacade.createTrainer(trainer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistrationResponse(created.getUsername(), created.getPassword()));
    }

    @Operation(summary = "Get trainer profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @GetMapping("/{username}")
    public ResponseEntity<TrainerProfileResponse> getTrainerProfile(
            @PathVariable("username") String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to get trainer profile: {}", username);
        authorize(authHeader, username);
        return gymFacade.getTrainer(username)
                .map(this::mapToTrainerProfileResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to update trainer profile: {}", username);
        authorize(authHeader, username);
        Trainer updated = gymFacade.updateTrainer(username, request.getFirstName(), request.getLastName(),
                request.getIsActive());
        return ResponseEntity.ok(mapToTrainerProfileResponse(updated));
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
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to get trainer trainings: {}", username);
        authorize(authHeader, username);
        List<Training> trainings = gymFacade.getTrainerTrainings(username, periodFrom, periodTo, traineeName);
        return ResponseEntity.ok(trainings.stream()
                .map(training -> TrainingResponse.builder()
                        .trainingName(training.getTrainingName())
                        .trainingDate(training.getTrainingDate())
                        .trainingType(training.getTrainingType())
                        .trainingDuration((int) training.getTrainingDuration().toMinutes())
                        .traineeName(training.getTrainee().getFirstName() + " " + training.getTrainee().getLastName())
                        .build())
                .collect(Collectors.toList()));
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
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to {} trainer: {}", request.getIsActive() ? "activate" : "deactivate", username);
        authorize(authHeader, username);
        gymFacade.activateDeactivateTrainer(username, request.getIsActive());
        return ResponseEntity.ok().build();
    }

    private TrainerProfileResponse mapToTrainerProfileResponse(Trainer trainer) {
        return TrainerProfileResponse.builder()
                .username(trainer.getUsername())
                .firstName(trainer.getFirstName())
                .lastName(trainer.getLastName())
                .specialization(trainer.getSpecialization())
                .isActive(trainer.isActive())
                .trainees(trainer.getTrainees() != null ? trainer.getTrainees().stream()
                        .map(trainee -> new TraineeInfo(trainee.getUsername(), trainee.getFirstName(), trainee.getLastName()))
                        .collect(Collectors.toList()) : List.of())
                .build();
    }

    private void authorize(String authHeader, String username) {
        jwtService.authenticate(authHeader).requireSelf(UserType.TRAINER, username);
    }
}
