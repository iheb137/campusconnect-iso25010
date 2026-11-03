package com.campusconnect.backend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.ReportedRequestResponse;
import com.campusconnect.backend.service.ReportService;

/**
 * Modération des publications signalées.
 * Accès : rôles MODERATOR et ADMIN (voir SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReportService reportService;

    public AdminController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Publications signalées. Par défaut, seules celles qui ont encore un signalement en attente ;
     * {@code ?pendingOnly=false} renvoie aussi l'historique (signalements traités ou rejetés).
     */
    @GetMapping("/reports")
    public List<ReportedRequestResponse> reportedRequests(
            @RequestParam(name = "pendingOnly", defaultValue = "true") boolean pendingOnly) {
        return reportService.listReportedRequests(pendingOnly);
    }

    @PostMapping("/requests/{id}/hide")
    public ReportedRequestResponse hide(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        return reportService.hide(id, Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/requests/{id}/restore")
    public ReportedRequestResponse restore(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        return reportService.restore(id, Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/requests/{id}/dismiss")
    public ReportedRequestResponse dismiss(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        return reportService.dismiss(id, Long.valueOf(jwt.getSubject()));
    }
}
