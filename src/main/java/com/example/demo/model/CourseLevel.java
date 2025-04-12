package com.example.demo.model;

public enum CourseLevel {
    DEBUTANT,
    INTERMEDIAIRE,
    AVANCE;

    public static CourseLevel fromString(String levelString) {
        switch(levelString.toUpperCase()) {
            case "DEBUTANT":
                return DEBUTANT;
            case "INTERMEDIAIRE":
                return INTERMEDIAIRE;
            case "AVANCE":
                return AVANCE;
            default:
                throw new IllegalArgumentException("Niveau de cours inconnu: " + levelString);
        }
    }
}
