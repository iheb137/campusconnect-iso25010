package com.campusconnect.backend.dto;

import java.time.Instant;
import java.util.List;

import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.RequestStatus;

/**
 * Tableau de bord personnel : demandes publiées, aides proposées et séances réalisées.
 */
public record DashboardResponse(
        Stats stats,
        List<MyRequest> myRequests,
        List<MyOffer> myOffers,
        List<Session> sessions) {

    /** Copies défensives non modifiables : l'objet renvoyé est immuable. */
    public DashboardResponse {
        myRequests = List.copyOf(myRequests);
        myOffers = List.copyOf(myOffers);
        sessions = List.copyOf(sessions);
    }

    public record Stats(
            long publishedRequests,
            long openRequests,
            long offersMade,
            long offersAccepted,
            long completedSessions,
            long ratingsReceived,
            Double averageRating) {
    }

    /** Demande publiée par l'utilisateur ({@code hidden} = masquée par un administrateur). */
    public record MyRequest(
            Long id,
            String title,
            SubjectResponse subject,
            Level level,
            RequestStatus status,
            boolean hidden,
            Instant createdAt,
            Instant completedAt,
            long offersCount,
            long pendingOffersCount) {
    }

    /** Aide proposée par l'utilisateur sur la demande d'un autre étudiant. */
    public record MyOffer(
            Long id,
            String message,
            OfferStatus status,
            Instant createdAt,
            Long requestId,
            String requestTitle,
            RequestStatus requestStatus,
            SubjectResponse subject,
            AuthorSummary requestAuthor) {
    }

    /** Séance terminée à laquelle l'utilisateur a participé. */
    public record Session(
            Long requestId,
            String title,
            SubjectResponse subject,
            Instant completedAt,
            SessionRole role,
            AuthorSummary partner,
            boolean ratedByMe) {
    }

    public enum SessionRole {
        AUTEUR,
        AIDANT
    }
}
