package com.campusconnect.backend.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Recherche filtrée. Les bornes de dates sont toujours renseignées par le service
     * (valeurs extrêmes si l'utilisateur ne filtre pas), ce qui évite les paramètres nuls typés.
     */
    @Query("""
            select a from AuditLog a
            where (:action is null or a.action = :action)
              and (:actorId is null or a.actorId = :actorId)
              and lower(coalesce(a.actorEmail, '')) like :email
              and a.createdAt >= :from and a.createdAt < :to
            """)
    Page<AuditLog> search(
            @Param("action") AuditAction action,
            @Param("actorId") Long actorId,
            @Param("email") String emailPattern,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    List<AuditLog> findByActorIdOrderByIdDesc(Long actorId);

    List<AuditLog> findByTargetTypeAndTargetIdOrderByIdDesc(String targetType, Long targetId);
}
