package com.gymcrm.controller;

import com.gymcrm.domain.Trainee;
import com.gymcrm.domain.Trainer;
import com.gymcrm.domain.Training;
import com.gymcrm.dto.ActivateDeactivateRequest;
import com.gymcrm.dto.RegistrationResponse;
import com.gymcrm.dto.TraineeProfileResponse;
import com.gymcrm.dto.TraineeRegistrationRequest;
import com.gymcrm.dto.TrainerInfo;
import com.gymcrm.dto.TrainingResponse;
import com.gymcrm.dto.UpdateTraineeRequest;
import com.gymcrm.dto.UpdateTraineeTrainersRequest;
import com.gymcrm.facade.GymFacade;
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
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/trainees")
public class TraineeController {
    private static final Logger log = LoggerFactory.getLogger(TraineeController.class);
    private final GymFacade gymFacade;

    public TraineeController(GymFacade gymFacade) {
        this.gymFacade = gymFacade;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> registerTrainee(@Valid @RequestBody TraineeRegistrationRequest request) {
        log.info("REST request to register trainee: {} {}", request.getFirstName(), request.getLastName());
        Trainee trainee = new Trainee(request.getFirstName(), request.getLastName(),
                request.getDateOfBirth(), request.getAddress());
        Trainee created = gymFacade.createTrainee(trainee);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistrationResponse(created.getUsername(), created.getPassword()));
    }

    @GetMapping("/{username}")
    public ResponseEntity<TraineeProfileResponse> getTraineeProfile(
            @PathVariable("username") String username,
            @RequestHeader(value = "Authorization", required = false)
            String authHeader) {

        log.info("REST request to get trainee profile: {}", username);
        String password = extractPassword(authHeader);
        return gymFacade.getTrainee(username, password)
                .map(this::mapToTraineeProfileResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{username}")
    public ResponseEntity<TraineeProfileResponse> updateTraineeProfile(
            @PathVariable("username") String username,
            @Valid @RequestBody UpdateTraineeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to update trainee profile: {}", username);
        String password = extractPassword(authHeader);
        Trainee trainee = new Trainee(request.getFirstName(), request.getLastName(), request.getDateOfBirth(), request.getAddress());
        trainee.setUsername(username);
        trainee.setActive(request.getIsActive());

        Trainee existing = gymFacade.getTrainee(username, password)
                .orElseThrow(() -> new SecurityException("Authentication failed or trainee not found"));
        trainee.setUserId(existing.getUserId());
        trainee.setTrainers(existing.getTrainers());
        trainee.setPassword(existing.getPassword());

        gymFacade.updateTrainee(username, password, trainee);
        return ResponseEntity.ok(mapToTraineeProfileResponse(trainee));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteTraineeProfile(
            @PathVariable("username") String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to delete trainee profile: {}", username);
        String password = extractPassword(authHeader);
        gymFacade.deleteTrainee(username, password);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{username}/trainers")
    public ResponseEntity<List<TrainerInfo>> updateTraineeTrainers(
            @PathVariable("username") String username,
            @Valid @RequestBody List<String> trainerUsernames,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to update trainers for trainee: {}", username);
        String password = extractPassword(authHeader);
        gymFacade.updateTraineeTrainers(username, password, trainerUsernames);

        Trainee updated = gymFacade.getTrainee(username, password).get();
        return ResponseEntity.ok(updated.getTrainers().stream()
                .map(trainer -> new TrainerInfo(trainer.getUsername(), trainer.getFirstName(),
                        trainer.getLastName(), trainer.getSpecialization()))
                .collect(Collectors.toList()));
    }

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
        String password = extractPassword(authHeader);
        List<Training> trainings = gymFacade.getTraineeTrainings(username, password, periodFrom, periodTo, trainerName, trainingType);
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

    @GetMapping("/{username}/unassigned-trainers")
    public ResponseEntity<List<TrainerInfo>> getUnassignedTrainers(
            @PathVariable("username") String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to get unassigned trainers for trainee: {}", username);
        String password = extractPassword(authHeader);
        Collection<Trainer> trainers = gymFacade.getTrainersNotAssignedToTrainee(username, password);
        return ResponseEntity.ok(trainers.stream()
                .map(trainer -> new TrainerInfo(trainer.getUsername(), trainer.getFirstName(),
                        trainer.getLastName(), trainer.getSpecialization()))
                .collect(Collectors.toList()));
    }

    @PatchMapping("/{username}/active")
    public ResponseEntity<Void> activateDeactivateTrainee(
            @PathVariable("username") String username,
            @Valid @RequestBody ActivateDeactivateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("REST request to {} trainee: {}", request.getIsActive() ? "activate" : "deactivate", username);
        String password = extractPassword(authHeader);
        gymFacade.activateDeactivateTrainee(username, password, request.getIsActive());
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

    private String extractPassword(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new SecurityException("Missing or invalid Authorization header");
        }
        String base64Credentials = authHeader.substring(6);
        byte[] decoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(decoded);
        // credentials = username:password
        int colonIndex = credentials.indexOf(":");
        if (colonIndex == -1) {
            throw new SecurityException("Invalid Authorization header format");
        }
        return credentials.substring(colonIndex + 1);
    }
}
