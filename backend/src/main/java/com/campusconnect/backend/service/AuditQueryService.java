package com.campusconnect.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.AuditLogResponse;
import com.campusconnect.backend.dto.PageResponse;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.repository.AuditLogRepository;

/** Consultation du journal d'audit (administrateur). */
@Service
public class AuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Instant MIN = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant MAX = Instant.parse("2100-01-01T00:00:00Z");

    private final AuditLogRepository auditLogRepository;
    private final ZoneId zone;

    public AuditQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.zone = ZoneId.systemDefault();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(
            AuditAction action, Long actorId, String email, LocalDate from, LocalDate to, int page, int size) {
        String pattern = "%" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT)) + "%";
        Instant start = from != null ? from.atStartOfDay(zone).toInstant() : MIN;
        Instant end = to != null ? to.plusDays(1).atStartOfDay(zone).toInstant() : MAX;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(
                auditLogRepository.search(action, actorId, pattern, start, end, pageable).map(AuditLogResponse::from));
    }
}
