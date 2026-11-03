package com.campusconnect.backend.dto;

import com.campusconnect.backend.entity.Role;

import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull(message = "Le rôle est obligatoire") Role role) {
}
