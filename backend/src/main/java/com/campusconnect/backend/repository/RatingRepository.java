package com.campusconnect.backend.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.backend.entity.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByRequestIdAndRaterId(Long requestId, Long raterId);

    @EntityGraph(attributePaths = { "rater", "rated", "request" })
    List<Rating> findByRequestIdOrderByCreatedAtAscIdAsc(Long requestId);

    List<Rating> findByRequestId(Long requestId);

    /** Moyenne et nombre d'évaluations sur la période : [moyenne, total]. */
    @Query("""
            select avg(r.score), count(r) from Rating r
            where r.createdAt >= :from and r.createdAt < :to
              and (:subjectId is null or r.request.subject.id = :subjectId)
            """)
    List<Object[]> averageScore(@Param("from") Instant from, @Param("to") Instant to, @Param("subjectId") Long subjectId);

    List<Rating> findByRaterIdOrRatedId(Long raterId, Long ratedId);

    /** Identifiants des demandes que l'utilisateur a déjà évaluées. */
    @Query("select r.request.id from Rating r where r.rater.id = :userId")
    List<Long> findRatedRequestIdsByRater(@Param("userId") Long userId);

    /** Évaluations reçues par l'utilisateur, avec la demande associée. */
    @EntityGraph(attributePaths = { "request" })
    List<Rating> findByRatedId(Long ratedId);
}
