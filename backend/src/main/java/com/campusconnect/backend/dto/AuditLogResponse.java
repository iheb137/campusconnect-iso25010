package com.campusconnect.backend.dto;

import java.time.Instant;

import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.AuditLog;

public record AuditLogResponse(
        Long id,
        Instant createdAt,
        Long actorId,
        String actorEmail,
        AuditAction action,
        String targetType,
        Long targetId,
        String details) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getCreatedAt(),
                log.getActorId(),
                log.getActorEmail(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetails());
    }
}
