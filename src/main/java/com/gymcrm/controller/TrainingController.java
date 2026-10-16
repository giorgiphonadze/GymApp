package com.gymcrm.controller;

import com.gymcrm.dto.AddTrainingRequest;
import com.gymcrm.dto.TrainingTypeResponse;
import com.gymcrm.facade.GymFacade;
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
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class TrainingController {
    private static final Logger log = LoggerFactory.getLogger(TrainingController.class);
    private final GymFacade gymFacade;

    public TrainingController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @PostMapping("/trainings")
    public ResponseEntity<Void> addTraining(
            @Valid @RequestBody AddTrainingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to add training: {}", request.getTrainingName());
        String password = extractPassword(authHeader);
        gymFacade.createTraining(request.getTraineeUsername(), password, request.getTrainerUsername(),
                request.getTrainingName(), request.getTrainingDate(), Duration.ofMinutes(request.getTrainingDuration()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/training-types")
    public ResponseEntity<List<TrainingTypeResponse>> getTrainingTypes() {
        log.info("REST request to get training types");
        return ResponseEntity.ok(gymFacade.getTrainingTypes().stream()
                .map(trainingType -> new TrainingTypeResponse(trainingType.getTrainingTypeName(),
                        (long) trainingType.ordinal() + 1))
                .collect(Collectors.toList()));
    }

    private String extractPassword(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new SecurityException("Missing or invalid Authorization header");
        }
        String base64Credentials = authHeader.substring(6);
        byte[] decoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(decoded);
        int colonIndex = credentials.indexOf(":");
        if (colonIndex == -1) {
            throw new SecurityException("Invalid Authorization header format");
        }
        return credentials.substring(colonIndex + 1);
    }
}
