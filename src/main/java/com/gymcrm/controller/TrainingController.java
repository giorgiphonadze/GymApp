package com.gymcrm.controller;

import com.gymcrm.config.OpenApiConfig;
import com.gymcrm.domain.UserType;
import com.gymcrm.dto.AddTrainingRequest;
import com.gymcrm.dto.TrainingTypeResponse;
import com.gymcrm.facade.GymFacade;
import com.gymcrm.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Trainings", description = "Trainings and training types")
public class TrainingController {
    private static final Logger log = LoggerFactory.getLogger(TrainingController.class);
    private final GymFacade gymFacade;
    private final JwtService jwtService;

    public TrainingController(GymFacade gymFacade, JwtService jwtService) {
        this.gymFacade = gymFacade;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Add training")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training added"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @PostMapping("/trainings")
    public ResponseEntity<Void> addTraining(
            @Valid @RequestBody AddTrainingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to add training: {}", request.getTrainingName());
        authorize(authHeader, request.getTraineeUsername());
        gymFacade.createTraining(request.getTraineeUsername(), request.getTrainerUsername(),
                request.getTrainingName(), request.getTrainingDate(), Duration.ofMinutes(request.getTrainingDuration()));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get training types",
            description = "Available to any authenticated trainee or trainer (Bearer token).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Training types returned"),
            @ApiResponse(responseCode = "401", description = "Missing/invalid credentials")
    })
    @GetMapping("/training-types")
    public ResponseEntity<List<TrainingTypeResponse>> getTrainingTypes(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to get training types");
        jwtService.authenticate(authHeader);
        return ResponseEntity.ok(gymFacade.getTrainingTypes().stream()
                .map(trainingType -> new TrainingTypeResponse(trainingType.getTrainingTypeName(),
                        (long) trainingType.ordinal() + 1))
                .collect(Collectors.toList()));
    }

    private void authorize(String authHeader, String traineeUsername) {
        jwtService.authenticate(authHeader).requireSelf(UserType.TRAINEE, traineeUsername);
    }
}
