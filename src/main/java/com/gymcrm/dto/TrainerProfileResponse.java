package com.gymcrm.dto;

import com.gymcrm.domain.TrainingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerProfileResponse {
    private String username;
    private String firstName;
    private String lastName;
    private TrainingType specialization;
    private boolean isActive;
    private List<TraineeInfo> trainees;
}
