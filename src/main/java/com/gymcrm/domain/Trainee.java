package com.gymcrm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "trainees")
@PrimaryKeyJoinColumn(name = "userId")
public class Trainee extends User{
    @Column
    private LocalDate dateOfBirth;

    @Column
    private String address;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "trainee_trainer",
            joinColumns = @JoinColumn(name = "trainee_id"),
            inverseJoinColumns = @JoinColumn(name = "trainer_id")
    )
    private Set<Trainer> trainers = new HashSet<>();

    @OneToMany(mappedBy = "trainee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Training> trainings;

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
