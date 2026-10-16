package com.gymcrm.dto;

import com.gymcrm.domain.TrainingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingResponse {
    private String trainingName;
    private LocalDate trainingDate;
    private TrainingType trainingType;
    private Integer trainingDuration;
    private String trainerName;
    private String traineeName;
}
