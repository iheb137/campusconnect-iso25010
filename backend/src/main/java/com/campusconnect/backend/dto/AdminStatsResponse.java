package com.campusconnect.backend.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Tableau de bord administrateur : indicateurs agrégés sur une période, filtrables par matière.
 */
public record AdminStatsResponse(
        LocalDate from,
        LocalDate to,
        Long subjectId,
        Map<String, Long> usersByRole,
        Map<String, Long> requestsByStatus,
        Map<String, Long> offersByStatus,
        long totalRequests,
        long completedSessions,
        Double completionRate,
        Double averageRating,
        long ratingsCount,
        long pendingReports,
        List<SubjectStat> bySubject,
        List<DayCount> requestsPerDay,
        List<HelperStat> topHelpers) {

    public record SubjectStat(Long subjectId, String name, long requests, long completed) {
    }

    public record DayCount(LocalDate day, long count) {
    }

    public record HelperStat(Long userId, String fullName, long sessions) {
    }
}
