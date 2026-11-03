package com.campusconnect.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Recherche des comptes par email ou nom (motif déjà en minuscules avec %), filtre de rôle facultatif. */
    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (lower(u.email) like :pattern or lower(u.fullName) like :pattern)
            """)
    Page<User> search(@Param("role") Role role, @Param("pattern") String pattern, Pageable pageable);

    /** Nombre de comptes par rôle : [role, total]. */
    @Query("select u.role, count(u) from User u group by u.role")
    List<Object[]> countByRole();

    /** Vrai si au moins un étudiant a déclaré cette matière dans ses compétences. */
    boolean existsBySkills_Id(Long subjectId);

    List<User> findByEmailStartingWith(String prefix);
}