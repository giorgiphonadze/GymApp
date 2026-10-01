package com.gymcrm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "trainers")
@PrimaryKeyJoinColumn(name = "userId")
public class Trainer extends User{
    @Enumerated(EnumType.STRING)
    @Column(name = "specialization", nullable = false)
    private TrainingType specialization;

    @ManyToMany(mappedBy = "trainers")
    private Set<Trainee> trainees;

    @OneToMany(mappedBy = "trainer")
    private List<Training> trainings;

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
