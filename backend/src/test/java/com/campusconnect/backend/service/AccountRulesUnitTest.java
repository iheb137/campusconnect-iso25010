package com.campusconnect.backend.service;

import static com.campusconnect.backend.service.Fixtures.student;
import static com.campusconnect.backend.service.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.campusconnect.backend.dto.LoginRequest;
import com.campusconnect.backend.dto.RegisterRequest;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.EmailAlreadyUsedException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.exception.InvalidCredentialsException;
import com.campusconnect.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitaire — comptes (RG-08, RG-10, RG-12)")
class AccountRulesUnitTest {

    @Mock UserRepository users;
    @Mock JwtService jwt;
    @Mock AuditService audit;
    @Mock LoginAttemptService attempts;

    @Nested
    @DisplayName("AuthService")
    class Auth {

        private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
        private AuthService service;

        @BeforeEach
        void setUp() {
            service = new AuthService(users, encoder, jwt, audit, attempts);
        }

        @Test
        @DisplayName("RG-08 : l'email est normalisé (minuscules, espaces) et unique")
        void duplicateEmailIsRejected() {
            when(users.existsByEmail("alice@test.tn")).thenReturn(true);
            assertThatThrownBy(() -> service.register(new RegisterRequest("  Alice@Test.TN ", "MotDePasse123", "Alice")))
                    .isInstanceOf(EmailAlreadyUsedException.class);
            verify(users, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("RG-08 : le mot de passe est stocké haché (BCrypt), jamais en clair ; rôle STUDENT par défaut")
        void passwordIsHashed() {
            when(users.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
            service.register(new RegisterRequest("bob@test.tn", "MotDePasse123", "  Bob  "));
            var captor = org.mockito.ArgumentCaptor.forClass(User.class);
            verify(users).saveAndFlush(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getPasswordHash()).isNotEqualTo("MotDePasse123").startsWith("$2");
            assertThat(encoder.matches("MotDePasse123", saved.getPasswordHash())).isTrue();
            assertThat(saved.getRole()).isEqualTo(Role.STUDENT);
            assertThat(saved.getFullName()).isEqualTo("Bob");
        }

        @Test
        @DisplayName("RG-12 : un échec de connexion est tracé même pour un email inconnu")
        void failedLoginIsAudited() {
            when(users.findByEmail("inconnu@test.tn")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.login(new LoginRequest("inconnu@test.tn", "x")))
                    .isInstanceOf(InvalidCredentialsException.class);
            verify(audit).recordIndependently(isNull(), eq("inconnu@test.tn"), eq(AuditAction.LOGIN_FAILED),
                    anyString(), isNull(), isNull());
            verify(attempts).recordFailure("inconnu@test.tn");
        }

        @Test
        @DisplayName("RG-10 : un compte désactivé ne peut pas se connecter, même avec le bon mot de passe")
        void disabledAccountCannotLogin() {
            User alice = student(5);
            alice.setPasswordHash(encoder.encode("MotDePasse123"));
            alice.setEnabled(false);
            when(users.findByEmail("alice@test.tn")).thenReturn(Optional.of(alice));
            assertThatThrownBy(() -> service.login(new LoginRequest("alice@test.tn", "MotDePasse123")))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessage("Ce compte est désactivé");
            verifyNoInteractions(jwt);
        }

        @Test
        @DisplayName("RG-10 : un mauvais mot de passe sur un compte désactivé ne révèle pas l'état du compte")
        void wrongPasswordOnDisabledAccountStaysGeneric() {
            User alice = student(5);
            alice.setPasswordHash(encoder.encode("MotDePasse123"));
            alice.setEnabled(false);
            when(users.findByEmail("alice@test.tn")).thenReturn(Optional.of(alice));
            assertThatThrownBy(() -> service.login(new LoginRequest("alice@test.tn", "mauvais")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("Connexion valide : jeton émis et succès tracé")
        void successfulLogin() {
            User alice = student(5);
            alice.setPasswordHash(encoder.encode("MotDePasse123"));
            when(users.findByEmail("alice@test.tn")).thenReturn(Optional.of(alice));
            when(jwt.generateToken(alice)).thenReturn("jeton");
            var response = service.login(new LoginRequest("ALICE@test.tn", "MotDePasse123"));
            assertThat(response.token()).isEqualTo("jeton");
            verify(attempts).recordSuccess("alice@test.tn");
            verify(audit).recordIndependently(eq(5L), eq("alice@test.tn"), eq(AuditAction.LOGIN_SUCCEEDED),
                    anyString(), eq(5L), isNull());
        }

        @Test
        @DisplayName("RG-13 : un email bloqué est refusé avant toute vérification et la tentative est tracée")
        void blockedEmailIsRefused() {
            org.mockito.Mockito.doThrow(new com.campusconnect.backend.exception.TooManyAttemptsException(60))
                    .when(attempts).checkAllowed("alice@test.tn");
            assertThatThrownBy(() -> service.login(new LoginRequest("alice@test.tn", "MotDePasse123")))
                    .isInstanceOf(com.campusconnect.backend.exception.TooManyAttemptsException.class);
            verifyNoInteractions(users);
            verify(audit).recordIndependently(isNull(), eq("alice@test.tn"), eq(AuditAction.LOGIN_FAILED),
                    anyString(), isNull(), eq("Bloqué : trop de tentatives"));
        }
    }

    @Nested
    @DisplayName("AdminUserService")
    class AdminUsers {

        private AdminUserService service;

        @BeforeEach
        void setUp() {
            service = new AdminUserService(users, audit);
        }

        @Test
        @DisplayName("RG-10 : un administrateur ne peut pas modifier son propre rôle")
        void adminCannotChangeOwnRole() {
            assertThatThrownBy(() -> service.changeRole(1L, Role.STUDENT, 1L)).isInstanceOf(ConflictException.class);
            verifyNoInteractions(users);
        }

        @Test
        @DisplayName("RG-10 : un administrateur ne peut pas désactiver son propre compte")
        void adminCannotDisableSelf() {
            assertThatThrownBy(() -> service.changeEnabled(1L, false, 1L)).isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("RG-12 : un changement de rôle est tracé avec l'ancien et le nouveau rôle")
        void roleChangeIsAudited() {
            User bob = student(2);
            when(users.findById(2L)).thenReturn(Optional.of(bob));
            service.changeRole(2L, Role.MODERATOR, 1L);
            assertThat(bob.getRole()).isEqualTo(Role.MODERATOR);
            verify(audit).record(1L, AuditAction.USER_ROLE_CHANGED, AuditService.USER, 2L, "STUDENT -> MODERATOR");
        }

        @Test
        @DisplayName("Un changement sans effet (même rôle) n'est pas tracé")
        void sameRoleIsNoop() {
            when(users.findById(2L)).thenReturn(Optional.of(user(2, Role.MODERATOR)));
            service.changeRole(2L, Role.MODERATOR, 1L);
            verify(audit, never()).record(anyLong(), any(), anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("RG-10 : désactiver un compte le rend inactif et trace l'action")
        void disableAccount() {
            User bob = student(2);
            when(users.findById(2L)).thenReturn(Optional.of(bob));
            service.changeEnabled(2L, false, 1L);
            assertThat(bob.isEnabled()).isFalse();
            verify(audit).record(1L, AuditAction.USER_DISABLED, AuditService.USER, 2L, bob.getEmail());
        }
    }
}
