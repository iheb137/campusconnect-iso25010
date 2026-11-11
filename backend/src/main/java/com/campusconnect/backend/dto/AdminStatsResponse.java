package com.campusconnect.backend.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    /** Copies défensives non modifiables (ordre conservé) : l'objet renvoyé est immuable. */
    public AdminStatsResponse {
        usersByRole = Collections.unmodifiableMap(new LinkedHashMap<>(usersByRole));
        requestsByStatus = Collections.unmodifiableMap(new LinkedHashMap<>(requestsByStatus));
        offersByStatus = Collections.unmodifiableMap(new LinkedHashMap<>(offersByStatus));
        bySubject = List.copyOf(bySubject);
        requestsPerDay = List.copyOf(requestsPerDay);
        topHelpers = List.copyOf(topHelpers);
    }

    public record SubjectStat(Long subjectId, String name, long requests, long completed) {
    }

    public record DayCount(LocalDate day, long count) {
    }

    public record HelperStat(Long userId, String fullName, long sessions) {
    }
}
