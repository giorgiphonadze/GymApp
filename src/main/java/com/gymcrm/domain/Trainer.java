package com.gymcrm.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Trainer extends User{
    private TrainingType specialization;

    public Trainer(){
        super();
    }
    public Trainer(String firstName, String lastName, TrainingType specialization){
        super(firstName,lastName);
        this.specialization = specialization;
    }

    @Override
    public String toString() {
        return "Trainer{id = " + getUserId() + ", Username = " + getUsername() + ", Specialization = " + specialization + "}";
    }
}
