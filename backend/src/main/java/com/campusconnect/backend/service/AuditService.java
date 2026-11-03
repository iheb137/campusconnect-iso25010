package com.campusconnect.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.AuditLog;
import com.campusconnect.backend.repository.AuditLogRepository;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Écriture du journal d'audit.
 * <ul>
 *   <li>{@link #record} s'exécute dans la transaction de l'action : si l'action est annulée, l'entrée l'est aussi
 *       (le journal ne ment jamais sur ce qui a été fait).</li>
 *   <li>{@link #recordIndependently} s'exécute dans sa propre transaction : sert aux événements qu'il faut garder
 *       même quand l'opération échoue (ex. connexion refusée).</li>
 * </ul>
 */
@Service
public class AuditService {

    public static final String USER = "USER";
    public static final String HELP_REQUEST = "HELP_REQUEST";
    public static final String HELP_OFFER = "HELP_OFFER";
    public static final String SUBJECT = "SUBJECT";

    private static final int MAX_DETAILS = 500;

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actorId, AuditAction action, String targetType, Long targetId, String details) {
        auditLogRepository.save(build(actorId, null, action, targetType, targetId, details));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(
            Long actorId, String actorEmail, AuditAction action, String targetType, Long targetId, String details) {
        auditLogRepository.save(build(actorId, actorEmail, action, targetType, targetId, details));
    }

    private AuditLog build(
            Long actorId, String actorEmail, AuditAction action, String targetType, Long targetId, String details) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        String email = actorEmail;
        if (email == null && actorId != null) {
            email = userRepository.findById(actorId).map(user -> user.getEmail()).orElse(null);
        }
        log.setActorEmail(email);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details == null || details.length() <= MAX_DETAILS ? details : details.substring(0, MAX_DETAILS));
        return log;
    }
}
