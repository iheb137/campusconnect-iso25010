package com.campusconnect.backend.dto;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.campusconnect.backend.entity.Level;

public record UpdateProfileRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères")
        String fullName,

        @Size(max = 150, message = "La spécialité ne doit pas dépasser 150 caractères")
        String specialty,

        Level level,

        @Size(max = 20, message = "20 compétences maximum")
        Set<@NotNull(message = "Identifiant de matière invalide") Long> skillIds) {

    /**
     * Copie défensive non modifiable. Les éléments null sont conservés volontairement :
     * c'est la validation (@NotNull) qui doit les refuser avec un message clair (400), pas une NPE.
     */
    public UpdateProfileRequest {
        skillIds = skillIds == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(skillIds));
    }
}