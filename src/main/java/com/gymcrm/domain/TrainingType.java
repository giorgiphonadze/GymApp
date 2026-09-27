package com.gymcrm.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class TrainingType {
    private Long id;
    private String trainingTypeName;

    public TrainingType(Long id, String trainingTypeName){
        this.id = id;
        this.trainingTypeName = trainingTypeName;
    }
    public TrainingType(String trainingType){
        this.trainingTypeName = trainingType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (!(obj instanceof TrainingType that)){
            return false;
        }
        return Objects.equals(trainingTypeName, that.trainingTypeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trainingTypeName);
    }

    @Override
    public String toString() {
        return "TrainingType{name ='" + trainingTypeName + "'}";
    }
}
