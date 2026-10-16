package com.gymcrm.dto;

import com.gymcrm.domain.TrainingType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainerInfo {
    private String username;
    private String firstName;
    private String lastName;
    private TrainingType specialization;
}
