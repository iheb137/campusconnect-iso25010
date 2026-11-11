package com.campusconnect.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.RequestStatus;

import jakarta.persistence.LockModeType;

public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long> {

    @EntityGraph(attributePaths = { "author", "subject" })
    Optional<HelpRequest> findWithDetailsById(Long id);

    /**
     * Charge la demande en la verrouillant (SELECT ... FOR UPDATE) jusqu'à la fin de la transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from HelpRequest r where r.id = :id")
    Optional<HelpRequest> findByIdForUpdate(@Param("id") Long id);

    List<HelpRequest> findByAuthorId(Long authorId);

    boolean existsBySubjectId(Long subjectId);

    /** Demandes publiées sur la période, par statut : [status, total]. */
    @Query("""
            select r.status, count(r) from HelpRequest r
            where r.createdAt >= :from and r.createdAt < :to
              and (:subjectId is null or r.subject.id = :subjectId)
            group by r.status
            """)
    List<Object[]> countByStatus(@Param("from") Instant from, @Param("to") Instant to, @Param("subjectId") Long subjectId);

    /** Par matière sur la période : [subjectId, nom, total, terminées]. */
    @Query("""
            select r.subject.id, r.subject.name, count(r),
                   sum(case when r.status = com.campusconnect.backend.entity.RequestStatus.TERMINEE then 1 else 0 end)
            from HelpRequest r
            where r.createdAt >= :from and r.createdAt < :to
              and (:subjectId is null or r.subject.id = :subjectId)
            group by r.subject.id, r.subject.name
            order by count(r) desc
            """)
    List<Object[]> countBySubject(@Param("from") Instant from, @Param("to") Instant to, @Param("subjectId") Long subjectId);

    /** Dates de publication sur la période (agrégées par jour dans le service). */
    @Query("""
            select r.createdAt from HelpRequest r
            where r.createdAt >= :from and r.createdAt < :to
              and (:subjectId is null or r.subject.id = :subjectId)
            """)
    List<Instant> findCreationDates(@Param("from") Instant from, @Param("to") Instant to, @Param("subjectId") Long subjectId);

    /** Demandes publiées par l'auteur (masquées comprises), les plus récentes d'abord. */
    @EntityGraph(attributePaths = { "subject" })
    List<HelpRequest> findByAuthorIdOrderByCreatedAtDescIdDesc(Long authorId);

    @EntityGraph(attributePaths = { "author", "subject" })
    @Query("""
            select r from HelpRequest r
            where r.hidden = false
              and (:subjectId is null or r.subject.id = :subjectId)
              and (:level is null or r.level = :level)
              and (:status is null or r.status = :status)
              and (lower(r.title) like :pattern escape '\\' or lower(r.description) like :pattern escape '\\')
            """)
    Page<HelpRequest> search(
            @Param("subjectId") Long subjectId,
            @Param("level") Level level,
            @Param("status") RequestStatus status,
            @Param("pattern") String pattern,
            Pageable pageable);
}