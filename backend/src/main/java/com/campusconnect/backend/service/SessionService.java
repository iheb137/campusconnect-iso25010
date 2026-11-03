package com.campusconnect.backend.service;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.HelpRequestResponse;
import com.campusconnect.backend.dto.RatingInput;
import com.campusconnect.backend.dto.RatingResponse;
import com.campusconnect.backend.dto.SessionRatingsResponse;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.Rating;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.RatingRepository;

/**
 * Fin du cycle de vie d'une demande : clôture de la séance puis évaluation par les participants.
 * Les participants d'une séance sont l'auteur de la demande et l'aidant dont la proposition a été acceptée.
 */
@Service
public class SessionService {

    private final HelpRequestRepository helpRequestRepository;
    private final HelpOfferRepository helpOfferRepository;
    private final RatingRepository ratingRepository;
    private final AuditService auditService;

    public SessionService(
            HelpRequestRepository helpRequestRepository,
            HelpOfferRepository helpOfferRepository,
            RatingRepository ratingRepository,
            AuditService auditService) {
        this.helpRequestRepository = helpRequestRepository;
        this.helpOfferRepository = helpOfferRepository;
        this.ratingRepository = ratingRepository;
        this.auditService = auditService;
    }

    /**
     * Passe une demande de AIDE_ACCEPTEE à TERMINEE. Seul l'auteur peut clôturer.
     */
    @Transactional
    public HelpRequestResponse complete(Long requestId, Long currentUserId) {
        // Verrou : une clôture ne peut pas se croiser avec une acceptation ou une autre clôture
        HelpRequest request = helpRequestRepository.findByIdForUpdate(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (!request.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("Seul l'auteur peut clôturer cette demande");
        }
        if (request.getStatus() == RequestStatus.TERMINEE) {
            throw new ConflictException("Cette demande est déjà terminée");
        }
        if (request.getStatus() != RequestStatus.AIDE_ACCEPTEE) {
            throw new ConflictException("Une demande ne peut être terminée qu'après l'acceptation d'une aide");
        }

        request.setStatus(RequestStatus.TERMINEE);
        request.setCompletedAt(Instant.now());
        auditService.record(currentUserId, AuditAction.SESSION_COMPLETED, AuditService.HELP_REQUEST, requestId, null);
        return HelpRequestResponse.from(request, currentUserId);
    }

    /**
     * Enregistre l'évaluation d'un participant sur l'autre participant, après la clôture de la séance.
     */
    @Transactional
    public RatingResponse rate(Long requestId, Long currentUserId, RatingInput input) {
        HelpRequest request = loadVisibleRequest(requestId);
        User helper = acceptedHelper(request);
        User author = request.getAuthor();

        boolean isAuthor = author.getId().equals(currentUserId);
        boolean isHelper = helper != null && helper.getId().equals(currentUserId);
        if (!isAuthor && !isHelper) {
            throw new ForbiddenOperationException("Seuls les participants de la séance peuvent l'évaluer");
        }
        if (request.getStatus() != RequestStatus.TERMINEE) {
            throw new ConflictException("L'évaluation n'est possible qu'après la clôture de la séance");
        }
        if (ratingRepository.existsByRequestIdAndRaterId(requestId, currentUserId)) {
            throw new ConflictException("Vous avez déjà évalué cette séance");
        }

        Rating rating = new Rating();
        rating.setRequest(request);
        rating.setRater(isAuthor ? author : helper);
        rating.setRated(isAuthor ? helper : author);
        rating.setScore(input.score());
        rating.setComment(normalizeComment(input.comment()));

        Rating saved;
        try {
            saved = ratingRepository.saveAndFlush(rating);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Vous avez déjà évalué cette séance");
        }
        auditService.record(currentUserId, AuditAction.RATING_SUBMITTED, AuditService.HELP_REQUEST, requestId,
                "Note " + input.score());
        return RatingResponse.from(saved);
    }

    /**
     * Évaluations d'une séance. Réservé aux participants.
     */
    @Transactional(readOnly = true)
    public SessionRatingsResponse ratings(Long requestId, Long currentUserId) {
        HelpRequest request = loadVisibleRequest(requestId);
        User helper = acceptedHelper(request);

        boolean isParticipant = request.getAuthor().getId().equals(currentUserId)
                || (helper != null && helper.getId().equals(currentUserId));
        if (!isParticipant) {
            throw new ForbiddenOperationException("Seuls les participants de la séance peuvent voir ses évaluations");
        }

        List<RatingResponse> ratings = ratingRepository.findByRequestIdOrderByCreatedAtAscIdAsc(requestId).stream()
                .map(RatingResponse::from)
                .toList();
        boolean alreadyRated = ratings.stream().anyMatch(rating -> rating.rater().id().equals(currentUserId));
        boolean canRate = request.getStatus() == RequestStatus.TERMINEE && !alreadyRated;
        return new SessionRatingsResponse(canRate, ratings);
    }

    private HelpRequest loadVisibleRequest(Long requestId) {
        return helpRequestRepository.findWithDetailsById(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
    }

    /** Aidant dont la proposition a été acceptée, ou null si aucune ne l'a été. */
    private User acceptedHelper(HelpRequest request) {
        return helpOfferRepository.findFirstByRequestIdAndStatus(request.getId(), OfferStatus.ACCEPTEE)
                .map(HelpOffer::getHelper)
                .orElse(null);
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}
