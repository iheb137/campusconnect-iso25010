package com.campusconnect.backend.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.LoginRequest;
import com.campusconnect.backend.dto.LoginResponse;
import com.campusconnect.backend.dto.RegisterRequest;
import com.campusconnect.backend.dto.UserResponse;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.EmailAlreadyUsedException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.exception.InvalidCredentialsException;
import com.campusconnect.backend.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    // Hachage factice : sert à faire le même travail quand l'email n'existe pas
    private final String dummyHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.dummyHash = passwordEncoder.encode("mot-de-passe-factice");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyUsedException();
        }
        auditService.record(saved.getId(), AuditAction.USER_REGISTERED, AuditService.USER, saved.getId(), null);
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalize(request.email());
        User user = userRepository.findByEmail(email).orElse(null);

        String hashToCheck = user != null ? user.getPasswordHash() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches) {
            auditService.recordIndependently(user != null ? user.getId() : null, email,
                    AuditAction.LOGIN_FAILED, AuditService.USER, user != null ? user.getId() : null, null);
            throw new InvalidCredentialsException();
        }
        // RG-10 : un compte désactivé ne peut plus se connecter (vérifié après le mot de passe,
        // pour ne rien révéler à quelqu'un qui ne connaît pas le mot de passe).
        if (!user.isEnabled()) {
            auditService.recordIndependently(user.getId(), email, AuditAction.LOGIN_FAILED, AuditService.USER,
                    user.getId(), "Compte désactivé");
            throw new ForbiddenOperationException("Ce compte est désactivé");
        }
        auditService.recordIndependently(user.getId(), email, AuditAction.LOGIN_SUCCEEDED, AuditService.USER,
                user.getId(), null);

        return new LoginResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpiresInSeconds(),
                UserResponse.from(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}