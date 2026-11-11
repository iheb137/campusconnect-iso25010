package com.campusconnect.backend.dto;

import java.time.Instant;
import java.util.List;

import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.ReportStatus;
import com.campusconnect.backend.entity.RequestStatus;

/**
 * Publication signalée, vue par l'administrateur, avec l'ensemble de ses signalements.
 */
public record ReportedRequestResponse(
        Long requestId,
        String title,
        String description,
        RequestStatus status,
        boolean hidden,
        Instant createdAt,
        SubjectResponse subject,
        AuthorSummary author,
        long pendingReports,
        List<ReportResponse> reports) {

    /** Copie défensive non modifiable. */
    public ReportedRequestResponse {
        reports = List.copyOf(reports);
    }

    /**
     * À appeler pendant une transaction : l'auteur et la matière sont chargés à la demande.
     */
    public static ReportedRequestResponse from(HelpRequest request, List<ReportResponse> reports) {
        long pending = reports.stream()
                .filter(report -> report.status() == ReportStatus.EN_ATTENTE)
                .count();
        return new ReportedRequestResponse(
                request.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.isHidden(),
                request.getCreatedAt(),
                SubjectResponse.from(request.getSubject()),
                new AuthorSummary(request.getAuthor().getId(), request.getAuthor().getFullName()),
                pending,
                reports);
    }
}
