package com.campusconnect.backend.dto;

import jakarta.validation.constraints.NotNull;

public record EnabledUpdateRequest(@NotNull(message = "L'état du compte est obligatoire") Boolean enabled) {
}
