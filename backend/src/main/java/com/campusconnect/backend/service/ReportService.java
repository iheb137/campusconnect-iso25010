package com.campusconnect.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.ReportInput;
import com.campusconnect.backend.dto.ReportResponse;
import com.campusconnect.backend.dto.ReportedRequestResponse;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Report;
import com.campusconnect.backend.entity.ReportStatus;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.ReportRepository;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Signalement des publications par les étudiants et modération par l'administrateur.
 * Une publication masquée disparaît de la liste et de toutes les opérations des étudiants
 * (le champ {@code hidden} est déjà pris en compte partout dans l'API).
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ReportService(
            ReportRepository reportRepository,
            HelpRequestRepository helpRequestRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.reportRepository = reportRepository;
        this.helpRequestRepository = helpRequestRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ===== Étudiant =====

    @Transactional
    public ReportResponse report(Long requestId, Long reporterId, ReportInput input) {
        HelpRequest request = helpRequestRepository.findWithDetailsById(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (request.getAuthor().getId().equals(reporterId)) {
            throw new ForbiddenOperationException("Vous ne pouvez pas signaler votre propre demande");
        }
        if (reportRepository.existsByReporterIdAndRequestId(reporterId, requestId)) {
            throw new ConflictException("Vous avez déjà signalé cette demande");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Report report = new Report();
        report.setReason(input.reason().trim());
        report.setRequest(request);
        report.setReporter(reporter);

        Report saved;
        try {
            saved = reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Vous avez déjà signalé cette demande");
        }
        auditService.record(reporterId, AuditAction.REQUEST_REPORTED, AuditService.HELP_REQUEST, requestId,
                report.getReason());
        return ReportResponse.from(saved);
    }

    // ===== Administrateur =====

    /**
     * Publications signalées, regroupées par demande, de la plus récemment signalée à la plus ancienne.
     *
     * @param pendingOnly si vrai, seules les publications ayant au moins un signalement en attente
     */
    @Transactional(readOnly = true)
    public List<ReportedRequestResponse> listReportedRequests(boolean pendingOnly) {
        Map<Long, HelpRequest> requests = new LinkedHashMap<>();
        Map<Long, List<ReportResponse>> reportsByRequest = new LinkedHashMap<>();
        for (Report report : reportRepository.findAllWithDetails()) {
            HelpRequest request = report.getRequest();
            requests.putIfAbsent(request.getId(), request);
            reportsByRequest.computeIfAbsent(request.getId(), id -> new ArrayList<>())
                    .add(ReportResponse.from(report));
        }

        return requests.values().stream()
                .map(request -> ReportedRequestResponse.from(request, reportsByRequest.get(request.getId())))
                .filter(item -> !pendingOnly || item.pendingReports() > 0)
                .toList();
    }

    /** Masque la publication et marque ses signalements en attente comme traités. */
    @Transactional
    public ReportedRequestResponse hide(Long requestId, Long moderatorId) {
        HelpRequest request = loadForUpdate(requestId);
        if (request.isHidden()) {
            throw new ConflictException("Cette publication est déjà masquée");
        }
        request.setHidden(true);
        resolvePending(requestId, ReportStatus.TRAITE);
        auditService.record(moderatorId, AuditAction.REQUEST_HIDDEN, AuditService.HELP_REQUEST, requestId,
                request.getTitle());
        return reportedRequest(request);
    }

    /** Rend de nouveau visible une publication masquée. */
    @Transactional
    public ReportedRequestResponse restore(Long requestId, Long moderatorId) {
        HelpRequest request = loadForUpdate(requestId);
        if (!request.isHidden()) {
            throw new ConflictException("Cette publication n'est pas masquée");
        }
        request.setHidden(false);
        auditService.record(moderatorId, AuditAction.REQUEST_RESTORED, AuditService.HELP_REQUEST, requestId,
                request.getTitle());
        return reportedRequest(request);
    }

    /** Rejette les signalements en attente : la publication reste visible. */
    @Transactional
    public ReportedRequestResponse dismiss(Long requestId, Long moderatorId) {
        HelpRequest request = loadForUpdate(requestId);
        int resolved = resolvePending(requestId, ReportStatus.REJETE);
        if (resolved == 0) {
            throw new ConflictException("Aucun signalement en attente pour cette publication");
        }
        auditService.record(moderatorId, AuditAction.REPORTS_DISMISSED, AuditService.HELP_REQUEST, requestId,
                resolved + " signalement(s)");
        return reportedRequest(request);
    }

    private HelpRequest loadForUpdate(Long requestId) {
        return helpRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
    }

    private int resolvePending(Long requestId, ReportStatus newStatus) {
        List<Report> pending = reportRepository.findByRequestIdAndStatus(requestId, ReportStatus.EN_ATTENTE);
        Instant now = Instant.now();
        for (Report report : pending) {
            report.setStatus(newStatus);
            report.setResolvedAt(now);
        }
        reportRepository.flush();
        return pending.size();
    }

    private ReportedRequestResponse reportedRequest(HelpRequest request) {
        List<ReportResponse> reports = reportRepository.findByRequestIdWithDetails(request.getId()).stream()
                .map(ReportResponse::from)
                .toList();
        return ReportedRequestResponse.from(request, reports);
    }
}
