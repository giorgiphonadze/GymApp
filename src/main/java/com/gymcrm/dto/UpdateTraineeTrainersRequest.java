package com.gymcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTraineeTrainersRequest {
    @NotBlank(message = "Trainee username is required")
    private String traineeUsername;
    @NotEmpty(message = "Trainers list cannot be empty")
    private List<String> trainerUsernames;
}
