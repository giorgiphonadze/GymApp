package com.gymcrm.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
public class Training {
    private Long id;
    private Long trainerId;
    private Long traineeId;
    private String trainingName;
    private TrainingType trainingType;
    private LocalDate trainingDate;
    private int trainingDuration; // minutes

    public Training(Long trainerId, Long traineeId, String trainingName, TrainingType trainingType, LocalDate trainingDate, int trainingDuration) {
        this.trainerId = trainerId;
        this.traineeId = traineeId;
        this.trainingName = trainingName;
        this.trainingType = trainingType;
        this.trainingDate = trainingDate;
        this.trainingDuration = trainingDuration;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Training training)) {
            return false;
        }
        return Objects.equals(id, training.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Training{id=" + id + ", name='" + trainingName + "', type=" + trainingType
                + ", trainerId=" + trainerId + ", traineeId=" + traineeId
                + ", date=" + trainingDate + ", duration=" + trainingDuration + "}";
    }
}
