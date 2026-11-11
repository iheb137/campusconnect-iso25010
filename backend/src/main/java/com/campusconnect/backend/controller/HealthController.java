package com.campusconnect.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sonde de disponibilité (fiabilité). Vérifie aussi la base de données :
 * 200 quand tout répond, 503 quand PostgreSQL est injoignable (utilisé par Docker Compose et les tests de reprise).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        boolean databaseUp = isDatabaseUp();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", databaseUp ? "ok" : "degraded");
        body.put("application", "CampusConnect");
        body.put("database", databaseUp ? "up" : "down");
        return ResponseEntity.status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean isDatabaseUp() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (DataAccessException e) {
            return false;
        }
    }
}
