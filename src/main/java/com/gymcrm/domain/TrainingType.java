package com.gymcrm.domain;

public enum TrainingType {
    YOGA("Yoga"),
    BJJ("BJJ"),
    CARDIO("Cardio"),
    STRENGTH("Strength"),
    ZUMBA("Zumba"),
    PILATES("Pilates"),
    BOXING("Boxing");

    private final String name;

    TrainingType(String name) {
        this.name = name;
    }

    public String getTrainingTypeName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
