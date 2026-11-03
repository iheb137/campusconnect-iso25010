package com.campusconnect.backend.dto;

import java.time.Instant;

import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;

public record AdminUserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean enabled,
        Level level,
        String specialty,
        Instant createdAt) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.getLevel(),
                user.getSpecialty(),
                user.getCreatedAt());
    }
}
