package com.campusconnect.backend.dto;

import java.util.List;

/**
 * Évaluations d'une séance, vues par l'un de ses participants.
 *
 * @param canRate vrai si l'utilisateur courant peut encore évaluer (séance terminée, pas encore évaluée)
 * @param ratings évaluations déjà laissées sur cette séance
 */
public record SessionRatingsResponse(boolean canRate, List<RatingResponse> ratings) {

    /** Copie défensive non modifiable. */
    public SessionRatingsResponse {
        ratings = List.copyOf(ratings);
    }
}
