package com.gymcrm.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class Trainee extends User{
    private LocalDate dateOfBirth;
    private String address;

    public Trainee(){
        super();
    }
    public Trainee(String firstName, String lastName, LocalDate dateOfBirth,String address){
        super(firstName, lastName);
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }
    @Override
    public String toString() {
        return "Trainee{id = " + getUserId() + ", username = " + getUsername() + ", dob = " + dateOfBirth + ", address = " + address + "}";
    }
}
