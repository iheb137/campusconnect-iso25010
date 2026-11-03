package com.campusconnect.backend.service;

import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.AdminUserResponse;
import com.campusconnect.backend.dto.PageResponse;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Gestion des comptes par l'administrateur : recherche, changement de rôle, activation / désactivation.
 * La « suppression » d'un compte est une désactivation : l'historique (demandes, évaluations, audit) est conservé.
 */
@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminUserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String query, Role role, int page, int size) {
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(userRepository.search(role, pattern, pageable).map(AdminUserResponse::from));
    }

    /** RG-10 : un administrateur ne peut pas modifier son propre rôle (pas de perte accidentelle des droits). */
    @Transactional
    public AdminUserResponse changeRole(Long userId, Role role, Long adminId) {
        if (userId.equals(adminId)) {
            throw new ConflictException("Vous ne pouvez pas modifier votre propre rôle");
        }
        User user = find(userId);
        Role previous = user.getRole();
        if (previous != role) {
            user.setRole(role);
            auditService.record(adminId, AuditAction.USER_ROLE_CHANGED, AuditService.USER, userId,
                    previous + " -> " + role);
        }
        return AdminUserResponse.from(user);
    }

    /** RG-10 : un administrateur ne peut pas désactiver son propre compte. */
    @Transactional
    public AdminUserResponse changeEnabled(Long userId, boolean enabled, Long adminId) {
        if (userId.equals(adminId) && !enabled) {
            throw new ConflictException("Vous ne pouvez pas désactiver votre propre compte");
        }
        User user = find(userId);
        if (user.isEnabled() != enabled) {
            user.setEnabled(enabled);
            auditService.record(adminId, enabled ? AuditAction.USER_ENABLED : AuditAction.USER_DISABLED,
                    AuditService.USER, userId, user.getEmail());
        }
        return AdminUserResponse.from(user);
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
