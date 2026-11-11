package com.campusconnect.backend.service;

import static com.campusconnect.backend.service.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.AuditLog;
import com.campusconnect.backend.repository.AuditLogRepository;
import com.campusconnect.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitaire — AuditService (RG-12)")
class AuditServiceUnitTest {

    @Mock AuditLogRepository logs;
    @Mock UserRepository users;
    @InjectMocks AuditService service;

    @Test
    @DisplayName("RG-12 : l'entrée contient qui (id + email résolu), quoi (action, cible) et le détail")
    void recordsWhoAndWhat() {
        when(users.findById(3L)).thenReturn(Optional.of(student(3)));
        service.record(3L, AuditAction.REQUEST_DELETED, AuditService.HELP_REQUEST, 10L, "Titre");
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(logs).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getActorId()).isEqualTo(3L);
        assertThat(log.getActorEmail()).isEqualTo("user3@test.tn");
        assertThat(log.getAction()).isEqualTo(AuditAction.REQUEST_DELETED);
        assertThat(log.getTargetType()).isEqualTo("HELP_REQUEST");
        assertThat(log.getTargetId()).isEqualTo(10L);
        assertThat(log.getDetails()).isEqualTo("Titre");
    }

    @Test
    @DisplayName("RG-12 : un détail trop long est tronqué à 500 caractères (pas d'erreur d'insertion)")
    void longDetailsAreTruncated() {
        service.recordIndependently(null, "x@test.tn", AuditAction.LOGIN_FAILED, AuditService.USER, null, "a".repeat(800));
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(logs).save(captor.capture());
        assertThat(captor.getValue().getDetails()).hasSize(500);
        assertThat(captor.getValue().getActorEmail()).isEqualTo("x@test.tn");
    }
}
