package com.campusconnect.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.AdminStatsResponse;
import com.campusconnect.backend.dto.AdminStatsResponse.DayCount;
import com.campusconnect.backend.dto.AdminStatsResponse.HelperStat;
import com.campusconnect.backend.dto.AdminStatsResponse.SubjectStat;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.ReportStatus;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.exception.BusinessValidationException;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.RatingRepository;
import com.campusconnect.backend.repository.ReportRepository;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Indicateurs agrégés pour le tableau de bord administrateur, filtrables par période et par matière.
 * Toutes les agrégations sont faites en base (group by), sauf la série par jour, limitée à la période choisie.
 */
@Service
public class AdminStatsService {

    static final int DEFAULT_DAYS = 30;
    static final int MAX_DAYS = 366;

    private final UserRepository userRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final HelpOfferRepository helpOfferRepository;
    private final RatingRepository ratingRepository;
    private final ReportRepository reportRepository;
    private final ZoneId zone = ZoneId.systemDefault();

    public AdminStatsService(
            UserRepository userRepository,
            HelpRequestRepository helpRequestRepository,
            HelpOfferRepository helpOfferRepository,
            RatingRepository ratingRepository,
            ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.helpRequestRepository = helpRequestRepository;
        this.helpOfferRepository = helpOfferRepository;
        this.ratingRepository = ratingRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats(LocalDate fromDate, LocalDate toDate, Long subjectId) {
        LocalDate to = toDate != null ? toDate : LocalDate.now(zone);
        LocalDate from = fromDate != null ? fromDate : to.minusDays(DEFAULT_DAYS - 1L);
        if (from.isAfter(to)) {
            throw new BusinessValidationException("from", "La date de début doit précéder la date de fin");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new BusinessValidationException("from", "La période ne doit pas dépasser " + MAX_DAYS + " jours");
        }
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();

        Map<String, Long> usersByRole = enumCounts(Role.values(), userRepository.countByRole());
        Map<String, Long> requestsByStatus =
                enumCounts(RequestStatus.values(), helpRequestRepository.countByStatus(start, end, subjectId));
        Map<String, Long> offersByStatus =
                enumCounts(OfferStatus.values(), helpOfferRepository.countByStatus(start, end, subjectId));

        long totalRequests = requestsByStatus.values().stream().mapToLong(Long::longValue).sum();
        long completed = requestsByStatus.get(RequestStatus.TERMINEE.name());
        Double completionRate = totalRequests == 0 ? null : round(100.0 * completed / totalRequests);

        Object[] rating = ratingRepository.averageScore(start, end, subjectId).stream().findFirst()
                .orElse(new Object[] { null, 0L });
        Double average = rating[0] == null ? null : round(((Number) rating[0]).doubleValue());
        long ratingsCount = rating[1] == null ? 0 : ((Number) rating[1]).longValue();

        List<SubjectStat> bySubject = helpRequestRepository.countBySubject(start, end, subjectId).stream()
                .map(row -> new SubjectStat((Long) row[0], (String) row[1], toLong(row[2]), toLong(row[3])))
                .toList();

        List<HelperStat> topHelpers = helpOfferRepository.topHelpers(start, end, subjectId, PageRequest.of(0, 5))
                .stream()
                .map(row -> new HelperStat((Long) row[0], (String) row[1], toLong(row[2])))
                .toList();

        return new AdminStatsResponse(
                from, to, subjectId,
                usersByRole, requestsByStatus, offersByStatus,
                totalRequests, completed, completionRate,
                average, ratingsCount,
                reportRepository.countByStatus(ReportStatus.EN_ATTENTE),
                bySubject,
                perDay(from, to, helpRequestRepository.findCreationDates(start, end, subjectId)),
                topHelpers);
    }

    /** Une valeur par jour de la période (0 si aucune demande), dans l'ordre chronologique. */
    private List<DayCount> perDay(LocalDate from, LocalDate to, List<Instant> dates) {
        Map<LocalDate, Long> counts = new TreeMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            counts.put(day, 0L);
        }
        for (Instant instant : dates) {
            counts.merge(LocalDate.ofInstant(instant, zone), 1L, Long::sum);
        }
        List<DayCount> result = new ArrayList<>();
        counts.forEach((day, count) -> result.add(new DayCount(day, count)));
        return result;
    }

    private static <E extends Enum<E>> Map<String, Long> enumCounts(E[] values, List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (E value : values) {
            counts.put(value.name(), 0L);
        }
        for (Object[] row : rows) {
            counts.put(((Enum<?>) row[0]).name(), toLong(row[1]));
        }
        return counts;
    }

    private static long toLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
