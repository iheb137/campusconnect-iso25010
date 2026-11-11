package com.campusconnect.backend.service;

import static com.campusconnect.backend.service.Fixtures.request;
import static com.campusconnect.backend.service.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.campusconnect.backend.dto.HelpRequestInput;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.ReportRepository;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitaire — HelpRequestService (RG-02)")
class HelpRequestServiceUnitTest {

    @Mock HelpRequestRepository requests;
    @Mock UserRepository users;
    @Mock SubjectRepository subjects;
    @Mock HelpOfferRepository offers;
    @Mock ReportRepository reports;
    @Mock AuditService audit;
    @InjectMocks HelpRequestService service;

    private static HelpRequestInput input() {
        return new HelpRequestInput("Nouveau titre", "Nouvelle description assez longue", "Jeudi", 1L, Level.L3);
    }

    @Test
    @DisplayName("RG-02 : l'auteur supprime sa demande ouverte (avec propositions et signalements) et l'action est tracée")
    void authorDeletesOpenRequest() {
        HelpRequest req = request(10, student(1), RequestStatus.OUVERTE);
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        when(offers.findByRequestId(10L)).thenReturn(List.of());
        when(reports.findByRequestId(10L)).thenReturn(List.of());
        service.delete(10L, 1L);
        verify(requests).delete(req);
        verify(audit).record(eq(1L), eq(AuditAction.REQUEST_DELETED), any(), eq(10L), eq("Demande 10"));
    }

    @Test
    @DisplayName("RG-02 : un autre étudiant ne peut pas supprimer la demande")
    void otherStudentCannotDelete() {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, student(1), RequestStatus.OUVERTE)));
        assertThatThrownBy(() -> service.delete(10L, 2L)).isInstanceOf(ForbiddenOperationException.class);
        verify(requests, never()).delete(any());
    }

    @ParameterizedTest(name = "RG-02 : suppression refusée au statut {0}")
    @EnumSource(value = RequestStatus.class, names = {"AIDE_ACCEPTEE", "TERMINEE"})
    void cannotDeleteOnceHelpAccepted(RequestStatus status) {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, student(1), status)));
        assertThatThrownBy(() -> service.delete(10L, 1L)).isInstanceOf(ConflictException.class);
        verify(requests, never()).delete(any());
    }

    @ParameterizedTest(name = "RG-02 : modification refusée au statut {0}")
    @EnumSource(value = RequestStatus.class, names = {"AIDE_ACCEPTEE", "TERMINEE"})
    void cannotUpdateOnceHelpAccepted(RequestStatus status) {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, student(1), status)));
        assertThatThrownBy(() -> service.update(10L, 1L, input())).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("RG-02 : seul l'auteur modifie sa demande")
    void onlyAuthorUpdates() {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, student(1), RequestStatus.OUVERTE)));
        assertThatThrownBy(() -> service.update(10L, 2L, input())).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Recherche : motif insensible à la casse, jokers saisis échappés")
    void likePatternEscapesWildcards() {
        org.assertj.core.api.Assertions.assertThat(HelpRequestService.likePattern(null)).isEqualTo("%");
        org.assertj.core.api.Assertions.assertThat(HelpRequestService.likePattern("  ")).isEqualTo("%");
        org.assertj.core.api.Assertions.assertThat(HelpRequestService.likePattern(" Graphes ")).isEqualTo("%graphes%");
        org.assertj.core.api.Assertions.assertThat(HelpRequestService.likePattern("50%_a\\b"))
                .isEqualTo("%50\\%\\_a\\\\b%");
    }
}
