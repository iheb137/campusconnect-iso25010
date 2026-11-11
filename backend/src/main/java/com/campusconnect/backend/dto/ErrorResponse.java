package com.campusconnect.backend.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {

    /** Copie défensive non modifiable (ordre des champs conservé). */
    public ErrorResponse {
        Map<String, String> copy = new LinkedHashMap<>();
        if (fieldErrors != null) {
            copy.putAll(fieldErrors);
        }
        fieldErrors = Collections.unmodifiableMap(copy);
    }

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, Map.of());
    }

    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}