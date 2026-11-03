package com.campusconnect.backend.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.OfferInput;
import com.campusconnect.backend.dto.OfferResponse;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.UserRepository;

@Service
public class OfferService {

    private final HelpOfferRepository helpOfferRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public OfferService(
            HelpOfferRepository helpOfferRepository,
            HelpRequestRepository helpRequestRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.helpOfferRepository = helpOfferRepository;
        this.helpRequestRepository = helpRequestRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public OfferResponse create(Long requestId, Long helperId, OfferInput input) {
        // Demande verrouillée : une proposition ne peut pas s'insérer au milieu d'une acceptation
        HelpRequest request = helpRequestRepository.findByIdForUpdate(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (request.getAuthor().getId().equals(helperId)) {
            throw new ForbiddenOperationException(
                    "Vous ne pouvez pas proposer votre aide sur votre propre demande");
        }
        if (request.getStatus() != RequestStatus.OUVERTE) {
            throw new ConflictException("Cette demande n'accepte plus de propositions");
        }
        if (helpOfferRepository.existsByHelperIdAndRequestId(helperId, requestId)) {
            throw new ConflictException("Vous avez déjà proposé votre aide sur cette demande");
        }

        User helper = userRepository.findById(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        HelpOffer offer = new HelpOffer();
        offer.setMessage(input.message().trim());
        offer.setHelper(helper);
        offer.setRequest(request);

        HelpOffer saved;
        try {
            saved = helpOfferRepository.saveAndFlush(offer);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Vous avez déjà proposé votre aide sur cette demande");
        }
        return OfferResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listForRequest(Long requestId, Long currentUserId) {
        HelpRequest request = helpRequestRepository.findWithDetailsById(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));

        if (!request.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("Seul l'auteur peut consulter les propositions");
        }

        return helpOfferRepository.findByRequestIdOrderByCreatedAtAscIdAsc(requestId).stream()
                .map(OfferResponse::from)
                .toList();
    }

    @Transactional
    public OfferResponse accept(Long offerId, Long currentUserId) {
        HelpRequest request = lockRequestOfOffer(offerId);
        requireAuthor(request, currentUserId,
                "Seul l'auteur de la demande peut accepter une proposition");
        requireOpen(request);
        HelpOffer offer = loadPendingOffer(offerId);

        offer.setStatus(OfferStatus.ACCEPTEE);
        request.setStatus(RequestStatus.AIDE_ACCEPTEE);

        for (HelpOffer other : helpOfferRepository.findByRequestId(request.getId())) {
            if (!other.getId().equals(offerId) && other.getStatus() == OfferStatus.EN_ATTENTE) {
                other.setStatus(OfferStatus.REFUSEE);
            }
        }
        auditService.record(currentUserId, AuditAction.OFFER_ACCEPTED, AuditService.HELP_OFFER, offerId,
                "Demande " + request.getId());
        return OfferResponse.from(offer);
    }

    @Transactional
    public OfferResponse reject(Long offerId, Long currentUserId) {
        HelpRequest request = lockRequestOfOffer(offerId);
        requireAuthor(request, currentUserId,
                "Seul l'auteur de la demande peut refuser une proposition");
        requireOpen(request);
        HelpOffer offer = loadPendingOffer(offerId);

        offer.setStatus(OfferStatus.REFUSEE);
        auditService.record(currentUserId, AuditAction.OFFER_REJECTED, AuditService.HELP_OFFER, offerId,
                "Demande " + request.getId());
        return OfferResponse.from(offer);
    }

    /**
     * Retrait d'une proposition par son auteur (RG-11) : seulement tant qu'elle est en attente
     * et que la demande est ouverte.
     */
    @Transactional
    public void withdraw(Long offerId, Long currentUserId) {
        HelpRequest request = lockRequestOfOffer(offerId);
        HelpOffer offer = helpOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposition introuvable"));
        if (!offer.getHelper().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("Seul l'auteur de la proposition peut la retirer");
        }
        if (offer.getStatus() != OfferStatus.EN_ATTENTE || request.getStatus() != RequestStatus.OUVERTE) {
            throw new ConflictException("Seule une proposition en attente sur une demande ouverte peut être retirée");
        }
        helpOfferRepository.delete(offer);
        auditService.record(currentUserId, AuditAction.OFFER_WITHDRAWN, AuditService.HELP_OFFER, offerId,
                "Demande " + request.getId());
    }

    /**
     * Trouve la demande d'une proposition et la verrouille. On ne charge volontairement pas la proposition
     * avant le verrou, pour ne pas lire un état périmé.
     */
    private HelpRequest lockRequestOfOffer(Long offerId) {
        Long requestId = helpOfferRepository.findRequestIdById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposition introuvable"));
        return helpRequestRepository.findByIdForUpdate(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Proposition introuvable"));
    }

    private void requireAuthor(HelpRequest request, Long userId, String message) {
        if (!request.getAuthor().getId().equals(userId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    private void requireOpen(HelpRequest request) {
        if (request.getStatus() != RequestStatus.OUVERTE) {
            throw new ConflictException("Cette demande n'est plus ouverte");
        }
    }

    private HelpOffer loadPendingOffer(Long offerId) {
        HelpOffer offer = helpOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposition introuvable"));
        if (offer.getStatus() != OfferStatus.EN_ATTENTE) {
            throw new ConflictException("Cette proposition a déjà été traitée");
        }
        return offer;
    }
}