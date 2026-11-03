package com.campusconnect.backend.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.AuditLogResponse;
import com.campusconnect.backend.dto.PageResponse;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.service.AuditQueryService;

/** Journal d'audit (rôle ADMIN) : filtres par action, acteur, email et période. */
@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AuditQueryService auditQueryService;

    public AdminAuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> search(
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "actorId", required = false) Long actorId,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return auditQueryService.search(action, actorId, email, from, to, page, size);
    }
}
