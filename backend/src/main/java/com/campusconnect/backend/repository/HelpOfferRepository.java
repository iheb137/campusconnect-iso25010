package com.campusconnect.backend.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.OfferStatus;

public interface HelpOfferRepository extends JpaRepository<HelpOffer, Long> {

    boolean existsByHelperIdAndRequestId(Long helperId, Long requestId);

    @EntityGraph(attributePaths = { "helper", "request" })
    List<HelpOffer> findByRequestIdOrderByCreatedAtAscIdAsc(Long requestId);

    List<HelpOffer> findByHelperId(Long helperId);

    List<HelpOffer> findByRequestId(Long requestId);

    /** Propositions faites sur la période, par statut : [status, total]. */
    @Query("""
            select o.status, count(o) from HelpOffer o
            where o.createdAt >= :from and o.createdAt < :to
              and (:subjectId is null or o.request.subject.id = :subjectId)
            group by o.status
            """)
    List<Object[]> countByStatus(@Param("from") Instant from, @Param("to") Instant to, @Param("subjectId") Long subjectId);

    /** Aidants les plus actifs (séances terminées sur la période) : [helperId, nom, séances]. */
    @Query("""
            select o.helper.id, o.helper.fullName, count(o) from HelpOffer o
            where o.status = com.campusconnect.backend.entity.OfferStatus.ACCEPTEE
              and o.request.status = com.campusconnect.backend.entity.RequestStatus.TERMINEE
              and o.request.completedAt >= :from and o.request.completedAt < :to
              and (:subjectId is null or o.request.subject.id = :subjectId)
            group by o.helper.id, o.helper.fullName
            order by count(o) desc
            """)
    List<Object[]> topHelpers(
            @Param("from") Instant from, @Param("to") Instant to, @Param("subjectId") Long subjectId, Pageable pageable);

    /**
     * Renvoie seulement l'identifiant de la demande liée à la proposition (une valeur simple,
     * qui ne charge pas la proposition en mémoire).
     */
    @Query("select o.request.id from HelpOffer o where o.id = :offerId")
    Optional<Long> findRequestIdById(@Param("offerId") Long offerId);

    /** Proposition acceptée d'une demande (au plus une), avec l'aidant. */
    @EntityGraph(attributePaths = { "helper" })
    Optional<HelpOffer> findFirstByRequestIdAndStatus(Long requestId, OfferStatus status);

    /** Propositions faites par l'utilisateur, les plus récentes d'abord, avec la demande et son auteur. */
    @EntityGraph(attributePaths = { "request", "request.author", "request.subject" })
    @Query("""
            select o from HelpOffer o
            where o.helper.id = :helperId and o.request.hidden = false
            order by o.createdAt desc, o.id desc
            """)
    List<HelpOffer> findVisibleByHelperId(@Param("helperId") Long helperId);

    /**
     * Séances réalisées par l'utilisateur : demandes terminées où il est l'auteur
     * ou l'aidant dont la proposition a été acceptée.
     */
    @EntityGraph(attributePaths = { "helper", "request", "request.author", "request.subject" })
    @Query("""
            select o from HelpOffer o
            where o.status = com.campusconnect.backend.entity.OfferStatus.ACCEPTEE
              and o.request.status = com.campusconnect.backend.entity.RequestStatus.TERMINEE
              and o.request.hidden = false
              and (o.request.author.id = :userId or o.helper.id = :userId)
            order by o.request.completedAt desc, o.request.id desc
            """)
    List<HelpOffer> findCompletedSessionsOf(@Param("userId") Long userId);

    /** Nombre de propositions par demande, pour les demandes d'un auteur : [requestId, total, enAttente]. */
    @Query("""
            select o.request.id, count(o),
                   sum(case when o.status = com.campusconnect.backend.entity.OfferStatus.EN_ATTENTE then 1 else 0 end)
            from HelpOffer o
            where o.request.author.id = :authorId
            group by o.request.id
            """)
    List<Object[]> countOffersByRequestForAuthor(@Param("authorId") Long authorId);
}
