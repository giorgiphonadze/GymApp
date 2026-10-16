package com.gymcrm.controller;

import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TraineeInfo;
import com.gymcrm.dto.TrainerInfo;
import com.gymcrm.dto.TrainerProfileResponse;
import com.gymcrm.dto.TrainerRegistrationRequest;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.UpdateTrainerRequest;
import com.gymcrm.facade.GymFacade;
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
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/trainers")
public class TrainerController {
    private static final Logger log = LoggerFactory.getLogger(TrainerController.class);
    private final GymFacade gymFacade;

    public TrainerController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> registerTrainer(@Valid @RequestBody TrainerRegistrationRequest request) {
        log.info("REST request to register trainer: {} {}", request.getFirstName(), request.getLastName());
        Trainer trainer = new Trainer(request.getFirstName(), request.getLastName(), request.getSpecialization());
        Trainer created = gymFacade.createTrainer(trainer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistrationResponse(created.getUsername(), created.getPassword()));
    }

    @GetMapping("/{username}")
    public ResponseEntity<TrainerProfileResponse> getTrainerProfile(
            @PathVariable("username") String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to get trainer profile: {}", username);
        String password = extractPassword(authHeader);
        return gymFacade.getTrainer(username, password)
                .map(this::mapToTrainerProfileResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{username}")
    public ResponseEntity<TrainerProfileResponse> updateTrainerProfile(
            @PathVariable("username") String username,
            @Valid @RequestBody UpdateTrainerRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to update trainer profile: {}", username);
        String password = extractPassword(authHeader);

        Trainer existing = gymFacade.getTrainer(username, password)
                .orElseThrow(() -> new SecurityException("Authentication failed or trainer not found"));

        Trainer trainer = new Trainer(request.getFirstName(), request.getLastName(), existing.getSpecialization());
        trainer.setUserId(existing.getUserId());
        trainer.setUsername(username);
        trainer.setActive(request.getIsActive());
        trainer.setTrainees(existing.getTrainees());
        trainer.setPassword(existing.getPassword());

        gymFacade.updateTrainer(username, password, trainer);
        return ResponseEntity.ok(mapToTrainerProfileResponse(trainer));
    }


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
        String password = extractPassword(authHeader);
        List<Training> trainings = gymFacade.getTrainerTrainings(username, password, periodFrom, periodTo, traineeName);
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

    @PatchMapping("/{username}/active")
    public ResponseEntity<Void> activateDeactivateTrainer(
            @PathVariable("username") String username,
            @Valid @RequestBody ActivateDeactivateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to {} trainer: {}", request.getIsActive() ? "activate" : "deactivate", username);
        String password = extractPassword(authHeader);
        gymFacade.activateDeactivateTrainer(username, password, request.getIsActive());
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
                        .map(t -> new TraineeInfo(t.getUsername(), t.getFirstName(), t.getLastName()))
                        .collect(Collectors.toList()) : List.of())
                .build();
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
