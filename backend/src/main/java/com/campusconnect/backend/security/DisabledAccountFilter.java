package com.campusconnect.backend.security;

import java.io.IOException;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import com.campusconnect.backend.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * RG-10 : un compte désactivé perd immédiatement l'accès à l'API, même avec un jeton encore valide.
 * Exécuté après l'authentification JWT : si le compte du jeton est désactivé (ou supprimé), la requête est refusée.
 */
public class DisabledAccountFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public DisabledAccountFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            boolean active;
            try {
                active = userRepository.findById(Long.valueOf(jwt.getSubject()))
                        .map(user -> user.isEnabled())
                        .orElse(false);
            } catch (DataAccessException | CannotCreateTransactionException databaseDown) {
                // Fiabilité : base injoignable → 503 propre, comme dans GlobalExceptionHandler
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setHeader("Retry-After", "5");
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":503,\"error\":\"Service Unavailable\","
                        + "\"message\":\"Service temporairement indisponible, réessayez dans quelques instants\","
                        + "\"fieldErrors\":{}}");
                return;
            }
            if (!active) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\","
                        + "\"message\":\"Ce compte est désactivé\",\"fieldErrors\":{}}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
