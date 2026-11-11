package com.campusconnect.backend.service;

import static com.campusconnect.backend.service.Fixtures.request;
import static com.campusconnect.backend.service.Fixtures.student;
import static com.campusconnect.backend.service.Fixtures.subject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.campusconnect.backend.dto.ReportInput;
import com.campusconnect.backend.dto.SubjectInput;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.ReportRepository;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitaire — matières et modération (RG-06, RG-09)")
class CatalogAndModerationUnitTest {

    @Mock SubjectRepository subjects;
    @Mock HelpRequestRepository requests;
    @Mock UserRepository users;
    @Mock ReportRepository reports;
    @Mock AuditService audit;

    @Nested
    @DisplayName("SubjectService")
    class Subjects {

        private SubjectService service;

        @BeforeEach
        void setUp() {
            service = new SubjectService(subjects, requests, users, audit);
        }

        @Test
        @DisplayName("RG-09 : le nom d'une matière est unique sans tenir compte de la casse")
        void nameIsUniqueIgnoringCase() {
            when(subjects.findAll()).thenReturn(List.of(subject(1, "Algorithmique")));
            assertThatThrownBy(() -> service.create(new SubjectInput("  ALGORITHMIQUE "), 9L))
                    .isInstanceOf(ConflictException.class);
            verify(subjects, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("RG-09 : renommer une matière avec son propre nom (casse différente) est permis")
        void renameSameSubjectAllowed() {
            Subject algo = subject(1, "Algorithmique");
            when(subjects.findById(1L)).thenReturn(Optional.of(algo));
            when(subjects.findAll()).thenReturn(List.of(algo));
            when(subjects.saveAndFlush(algo)).thenReturn(algo);
            service.update(1L, new SubjectInput("algorithmique"), 9L);
            assertThat(algo.getName()).isEqualTo("algorithmique");
            verify(audit).record(9L, AuditAction.SUBJECT_UPDATED, AuditService.SUBJECT, 1L,
                    "Algorithmique -> algorithmique");
        }

        @Test
        @DisplayName("RG-09 : une matière utilisée par une demande ne peut pas être supprimée")
        void usedSubjectCannotBeDeleted() {
            when(subjects.findById(1L)).thenReturn(Optional.of(subject(1, "Algorithmique")));
            when(requests.existsBySubjectId(1L)).thenReturn(true);
            assertThatThrownBy(() -> service.delete(1L, 9L)).isInstanceOf(ConflictException.class);
            verify(subjects, never()).delete(any());
        }

        @Test
        @DisplayName("RG-09 : une matière choisie comme compétence d'un profil ne peut pas être supprimée")
        void subjectUsedAsSkillCannotBeDeleted() {
            when(subjects.findById(1L)).thenReturn(Optional.of(subject(1, "Algorithmique")));
            when(users.existsBySkills_Id(1L)).thenReturn(true);
            assertThatThrownBy(() -> service.delete(1L, 9L)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("ReportService")
    class Moderation {

        private ReportService service;

        @BeforeEach
        void setUp() {
            service = new ReportService(reports, requests, users, audit);
        }

        @Test
        @DisplayName("RG-06 : on ne peut pas signaler sa propre demande")
        void cannotReportOwnRequest() {
            when(requests.findWithDetailsById(10L)).thenReturn(Optional.of(request(10, student(1), RequestStatus.OUVERTE)));
            assertThatThrownBy(() -> service.report(10L, 1L, new ReportInput("Contenu inapproprié")))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("RG-06 : un utilisateur ne signale une demande qu'une seule fois")
        void reportOnlyOnce() {
            when(requests.findWithDetailsById(10L)).thenReturn(Optional.of(request(10, student(1), RequestStatus.OUVERTE)));
            when(reports.existsByReporterIdAndRequestId(2L, 10L)).thenReturn(true);
            assertThatThrownBy(() -> service.report(10L, 2L, new ReportInput("Contenu inapproprié")))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("RG-06 : masquer une publication déjà masquée est refusé")
        void cannotHideTwice() {
            HelpRequest hidden = request(10, student(1), RequestStatus.OUVERTE);
            hidden.setHidden(true);
            when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(hidden));
            assertThatThrownBy(() -> service.hide(10L, 7L)).isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("RG-06 : rejeter des signalements sans signalement en attente est refusé")
        void dismissWithoutPendingReports() {
            when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, student(1), RequestStatus.OUVERTE)));
            when(reports.findByRequestIdAndStatus(any(), any())).thenReturn(List.of());
            assertThatThrownBy(() -> service.dismiss(10L, 7L)).isInstanceOf(ConflictException.class);
        }
    }
}
