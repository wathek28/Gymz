package com.example.demo.model;

import java.util.Arrays;
import java.util.List;

public enum Role {
    GYMZER,
    COACH,

    GYM,

    ADMIN,;

    // Retourne tous les rôles en tant que liste
    public static List<String> getAllRoles() {
        return Arrays.asList(GYMZER.name(), ADMIN.name(), GYM.name(), COACH.name());
    }

    // Méthode pour convertir une chaîne de caractères en rôle
    public static Role fromString(String roleString) {
        switch (roleString.toUpperCase()) {
            case "GYMZER":
                return GYMZER;
            case "ADMIN":
                return ADMIN;
            case "GYM":
                return GYM;
            case "COACH":
                return COACH;
            default:
                throw new IllegalArgumentException("Rôle inconnu : " + roleString);
        }
    }
}
