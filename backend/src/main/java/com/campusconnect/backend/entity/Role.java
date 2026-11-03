package com.campusconnect.backend.entity;

public enum Role {
    /** Étudiant : publie des demandes, propose son aide, évalue. */
    STUDENT,
    /** Modérateur : traite les publications signalées. */
    MODERATOR,
    /** Administrateur : gère les comptes, les matières, consulte l'audit et les statistiques ; modère aussi. */
    ADMIN
}
