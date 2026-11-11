package com.campusconnect.backend.dto;

import java.util.Comparator;
import java.util.List;

import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        String specialty,
        Level level,
        List<SubjectResponse> skills) {

    /** Copie défensive non modifiable. */
    public UserResponse {
        skills = List.copyOf(skills);
    }

    /**
     * À appeler pendant une transaction : la collection des compétences est chargée à la demande.
     */
    public static UserResponse from(User user) {
        List<SubjectResponse> skills = user.getSkills().stream()
                .map(SubjectResponse::from)
                .sorted(Comparator.comparing(SubjectResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getSpecialty(),
                user.getLevel(),
                skills);
    }
}