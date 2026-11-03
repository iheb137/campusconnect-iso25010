package com.campusconnect.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.OfferResponse;
import com.campusconnect.backend.service.OfferService;

@RestController
@RequestMapping("/api/offers")
public class OfferActionController {

    private final OfferService offerService;

    public OfferActionController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping("/{id}/accept")
    public OfferResponse accept(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id) {
        return offerService.accept(id, currentUserId(jwt));
    }

    @PostMapping("/{id}/reject")
    public OfferResponse reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id) {
        return offerService.reject(id, currentUserId(jwt));
    }

    /** Retrait d'une proposition en attente par son auteur (RG-11). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id) {
        offerService.withdraw(id, currentUserId(jwt));
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}