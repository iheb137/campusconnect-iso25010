package com.campusconnect.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valeur invalide";
            fieldErrors.putIfAbsent(error.getField(), message);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.BAD_REQUEST, "Les données envoyées sont invalides", fieldErrors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.BAD_REQUEST, "Les paramètres envoyés sont invalides",
                        Map.of(ex.getName(), "Valeur invalide")));
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleBusinessValidation(BusinessValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.BAD_REQUEST, "Les données envoyées sont invalides",
                        Map.of(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.BAD_REQUEST, "Le corps de la requête est illisible"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleForbidden(ForbiddenOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.FORBIDDEN, ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    /** RG-13 : trop d'échecs de connexion pour un même compte. */
    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleTooManyAttempts(TooManyAttemptsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }

    /**
     * Fiabilité : base de données injoignable → 503 propre (le client peut réessayer),
     * jamais de trace technique renvoyée au client.
     */
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleDatabaseDown(Exception ex) {
        LOG.error("Base de données indisponible : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "5")
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE,
                        "Service temporairement indisponible, réessayez dans quelques instants"));
    }

    /**
     * Toute autre erreur : on garde le code HTTP des exceptions standard de Spring (404, 405, 415…)
     * et on masque les détails internes des erreurs inattendues (500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<com.campusconnect.backend.dto.ErrorResponse> handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode code = springError.getStatusCode();
            HttpStatus status = HttpStatus.resolve(code.value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return ResponseEntity.status(status)
                    .body(com.campusconnect.backend.dto.ErrorResponse.of(status, status.getReasonPhrase()));
        }
        LOG.error("Erreur inattendue", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.campusconnect.backend.dto.ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Une erreur inattendue est survenue"));
    }
}
