package com.campusconnect.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.backend.entity.Report;
import com.campusconnect.backend.entity.ReportStatus;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndRequestId(Long reporterId, Long requestId);

    List<Report> findByRequestId(Long requestId);

    List<Report> findByReporterId(Long reporterId);

    List<Report> findByRequestIdAndStatus(Long requestId, ReportStatus status);

    long countByStatus(ReportStatus status);

    /** Tous les signalements, du plus récent au plus ancien, avec la demande, son auteur et le signaleur. */
    @EntityGraph(attributePaths = { "reporter", "request", "request.author", "request.subject" })
    @Query("select r from Report r order by r.createdAt desc, r.id desc")
    List<Report> findAllWithDetails();

    /** Signalements d'une demande, du plus récent au plus ancien. */
    @EntityGraph(attributePaths = { "reporter", "request", "request.author", "request.subject" })
    @Query("select r from Report r where r.request.id = :requestId order by r.createdAt desc, r.id desc")
    List<Report> findByRequestIdWithDetails(@Param("requestId") Long requestId);
}
