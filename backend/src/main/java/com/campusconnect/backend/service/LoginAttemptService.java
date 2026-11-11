package com.campusconnect.backend.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.campusconnect.backend.exception.TooManyAttemptsException;

/**
 * RG-13 : protection contre la force brute. Après {@code maxAttempts} échecs de connexion
 * sur le même email pendant la fenêtre, les tentatives suivantes sont refusées (429) jusqu'à la fin du blocage.
 * Une connexion réussie remet le compteur à zéro. Stockage en mémoire (une seule instance du backend).
 */
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant windowStart, Instant blockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.block-minutes:15}") long blockMinutes) {
        this(maxAttempts, Duration.ofMinutes(blockMinutes), Clock.systemUTC());
    }

    LoginAttemptService(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    /** Lève une exception 429 si l'email est actuellement bloqué. */
    public void checkAllowed(String email) {
        Attempts current = attempts.get(email);
        Instant now = clock.instant();
        if (current != null && current.blockedUntil() != null && now.isBefore(current.blockedUntil())) {
            long seconds = Math.max(1, Duration.between(now, current.blockedUntil()).toSeconds());
            throw new TooManyAttemptsException(seconds);
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        attempts.compute(email, (key, current) -> {
            if (current == null || now.isAfter(current.windowStart().plus(window))
                    || (current.blockedUntil() != null && !now.isBefore(current.blockedUntil()))) {
                current = new Attempts(0, now, null);
            }
            int failures = current.failures() + 1;
            Instant blockedUntil = failures >= maxAttempts ? now.plus(window) : null;
            return new Attempts(failures, current.windowStart(), blockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }
}
