package com.campusconnect.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.HelpRequestInput;
import com.campusconnect.backend.dto.HelpRequestResponse;
import com.campusconnect.backend.dto.PageResponse;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.BusinessValidationException;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.ReportRepository;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

@Service
public class HelpRequestService {

    private static final int MAX_PAGE_SIZE = 50;

    private final HelpRequestRepository helpRequestRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final HelpOfferRepository helpOfferRepository;
    private final ReportRepository reportRepository;
    private final AuditService auditService;

    public HelpRequestService(
            HelpRequestRepository helpRequestRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            HelpOfferRepository helpOfferRepository,
            ReportRepository reportRepository,
            AuditService auditService) {
        this.helpRequestRepository = helpRequestRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.helpOfferRepository = helpOfferRepository;
        this.reportRepository = reportRepository;
        this.auditService = auditService;
    }

    @Transactional
    public HelpRequestResponse create(Long authorId, HelpRequestInput input) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Subject subject = findSubject(input.subjectId());

        HelpRequest request = new HelpRequest();
        applyInput(request, input, subject);
        request.setAuthor(author);

        helpRequestRepository.save(request);
        return HelpRequestResponse.from(request, authorId);
    }

    @Transactional(readOnly = true)
    public HelpRequestResponse getById(Long requestId, Long currentUserId) {
        HelpRequest request = helpRequestRepository.findWithDetailsById(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        return HelpRequestResponse.from(request, currentUserId);
    }

    @Transactional
    public HelpRequestResponse update(Long requestId, Long currentUserId, HelpRequestInput input) {
        HelpRequest request = helpRequestRepository.findByIdForUpdate(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (!request.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("Seul l'auteur peut modifier cette demande");
        }
        if (request.getStatus() != RequestStatus.OUVERTE) {
            throw new ConflictException("Seule une demande ouverte peut être modifiée");
        }

        applyInput(request, input, findSubject(input.subjectId()));
        return HelpRequestResponse.from(request, currentUserId);
    }

    /**
     * Suppression d'une demande par son auteur (RG-02) : seulement tant qu'elle est ouverte.
     * Les propositions et signalements associés sont supprimés avec elle.
     */
    @Transactional
    public void delete(Long requestId, Long currentUserId) {
        HelpRequest request = helpRequestRepository.findByIdForUpdate(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (!request.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("Seul l'auteur peut supprimer cette demande");
        }
        if (request.getStatus() != RequestStatus.OUVERTE) {
            throw new ConflictException("Seule une demande ouverte peut être supprimée");
        }

        String title = request.getTitle();
        helpOfferRepository.deleteAll(helpOfferRepository.findByRequestId(requestId));
        reportRepository.deleteAll(reportRepository.findByRequestId(requestId));
        helpRequestRepository.delete(request);
        auditService.record(currentUserId, AuditAction.REQUEST_DELETED, AuditService.HELP_REQUEST, requestId, title);
    }

    @Transactional(readOnly = true)
    public PageResponse<HelpRequestResponse> search(
            Long subjectId,
            Level level,
            RequestStatus status,
            int page,
            int size,
            Long currentUserId) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(safePage, safeSize, newestFirst);

        Page<HelpRequestResponse> result = helpRequestRepository
                .search(subjectId, level, status, pageable)
                .map(request -> HelpRequestResponse.from(request, currentUserId));
        return PageResponse.from(result);
    }

    private Subject findSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BusinessValidationException("subjectId", "Matière inconnue"));
    }

    private void applyInput(HelpRequest request, HelpRequestInput input, Subject subject) {
        request.setTitle(input.title().trim());
        request.setDescription(input.description().trim());
        request.setAvailability(input.availability().trim());
        request.setLevel(input.level());
        request.setSubject(subject);
    }
}