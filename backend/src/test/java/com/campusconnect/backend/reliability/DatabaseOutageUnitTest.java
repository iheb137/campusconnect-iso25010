package com.campusconnect.backend.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.campusconnect.backend.controller.HealthController;
import com.campusconnect.backend.exception.GlobalExceptionHandler;

/** Fiabilité : comportement quand PostgreSQL est injoignable (sans arrêter de vraie base). */
@DisplayName("Fiabilité — panne de base de données (unitaire)")
class DatabaseOutageUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("REL-05 : base injoignable → /api/health répond 503 avec database=down")
    void healthReportsDatabaseDown() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new CannotGetJdbcConnectionException("Connection refused"));
        var response = new HealthController(jdbc).health();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("database", "down").containsEntry("status", "degraded");
    }

    @Test
    @DisplayName("REL-05 : base joignable → /api/health répond 200 avec database=up")
    void healthReportsDatabaseUp() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        var response = new HealthController(jdbc).health();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("status", "ok", "application", "CampusConnect", "database", "up"));
    }

    @Test
    @DisplayName("REL-06 : transaction impossible (base arrêtée) → 503 + Retry-After, sans détail technique")
    void databaseDownBecomes503() {
        var response = handler.handleDatabaseDown(
                new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("5");
        assertThat(response.getBody().message()).doesNotContain("JPA").contains("temporairement indisponible");
    }

    @Test
    @DisplayName("Erreur inattendue → 500 générique, sans le message interne")
    void unexpectedErrorIsGeneric() {
        var response = handler.handleUnexpected(new IllegalStateException("détail interne sensible"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Une erreur inattendue est survenue");
    }

    @Test
    @DisplayName("Les erreurs standard de Spring gardent leur code HTTP (404, 405)")
    void springErrorsKeepTheirStatus() {
        assertThat(handler.handleUnexpected(new NoResourceFoundException(HttpMethod.GET, "/api/x", "api/x"))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleUnexpected(new HttpRequestMethodNotSupportedException("PATCH"))
                .getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("REL-06 : base injoignable pendant le contrôle du compte (filtre de sécurité) → 503 JSON")
    void securityFilterReturns503WhenDatabaseIsDown() throws Exception {
        var users = mock(com.campusconnect.backend.repository.UserRepository.class);
        when(users.findById(org.mockito.ArgumentMatchers.anyLong()))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Connection refused"));
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("t").header("alg", "HS256").subject("7").build();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt));
        try {
            var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/requests");
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            var chain = new org.springframework.mock.web.MockFilterChain();
            new com.campusconnect.backend.security.DisabledAccountFilter(users).doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(response.getHeader("Retry-After")).isEqualTo("5");
            assertThat(response.getContentAsString()).contains("temporairement indisponible").doesNotContain("Connection refused");
            assertThat(chain.getRequest()).isNull();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
