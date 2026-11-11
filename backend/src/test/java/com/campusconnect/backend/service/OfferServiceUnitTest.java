package com.campusconnect.backend.service;

import static com.campusconnect.backend.service.Fixtures.offer;
import static com.campusconnect.backend.service.Fixtures.request;
import static com.campusconnect.backend.service.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.campusconnect.backend.dto.OfferInput;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitaire — OfferService (RG-01, RG-03, RG-04, RG-11)")
class OfferServiceUnitTest {

    @Mock HelpOfferRepository offers;
    @Mock HelpRequestRepository requests;
    @Mock UserRepository users;
    @Mock AuditService audit;
    @InjectMocks OfferService service;

    private final User author = student(1);
    private final User helper = student(2);

    @Test
    @DisplayName("RG-01 : un étudiant ne peut pas proposer son aide sur sa propre demande")
    void cannotOfferOnOwnRequest() {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, author, RequestStatus.OUVERTE)));
        assertThatThrownBy(() -> service.create(10L, 1L, new OfferInput("Je peux aider")))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(offers, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("RG-01 : une demande non ouverte n'accepte plus de propositions")
    void closedRequestRejectsOffers() {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, author, RequestStatus.AIDE_ACCEPTEE)));
        assertThatThrownBy(() -> service.create(10L, 2L, new OfferInput("Je peux aider")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("n'accepte plus");
    }

    @Test
    @DisplayName("RG-01 : une seule proposition par aidant et par demande")
    void oneOfferPerHelper() {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, author, RequestStatus.OUVERTE)));
        when(offers.existsByHelperIdAndRequestId(2L, 10L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(10L, 2L, new OfferInput("Je peux aider")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà proposé");
    }

    @Test
    @DisplayName("RG-01 : une violation d'unicité concurrente est traduite en conflit (409)")
    void concurrentDuplicateBecomesConflict() {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, author, RequestStatus.OUVERTE)));
        when(users.findById(2L)).thenReturn(Optional.of(helper));
        when(offers.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk"));
        assertThatThrownBy(() -> service.create(10L, 2L, new OfferInput("Je peux aider")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("RG-01 : proposition valide enregistrée avec un message nettoyé")
    void validOfferIsSaved() {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, author, RequestStatus.OUVERTE)));
        when(users.findById(2L)).thenReturn(Optional.of(helper));
        when(offers.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var response = service.create(10L, 2L, new OfferInput("  Je peux aider  "));
        assertThat(response.message()).isEqualTo("Je peux aider");
        assertThat(response.status()).isEqualTo(OfferStatus.EN_ATTENTE);
    }

    @Test
    @DisplayName("Une demande masquée par la modération est introuvable")
    void hiddenRequestIsNotFound() {
        HelpRequest hidden = request(10, author, RequestStatus.OUVERTE);
        hidden.setHidden(true);
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(hidden));
        assertThatThrownBy(() -> service.create(10L, 2L, new OfferInput("Je peux aider")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("RG-03/RG-04 : accepter passe la demande en AIDE_ACCEPTEE et refuse les autres propositions en attente")
    void acceptRejectsOtherPendingOffers() {
        HelpRequest req = request(10, author, RequestStatus.OUVERTE);
        HelpOffer chosen = offer(100, helper, req, OfferStatus.EN_ATTENTE);
        HelpOffer other = offer(101, student(3), req, OfferStatus.EN_ATTENTE);
        HelpOffer alreadyRefused = offer(102, student(4), req, OfferStatus.REFUSEE);
        when(offers.findRequestIdById(100L)).thenReturn(Optional.of(10L));
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        when(offers.findById(100L)).thenReturn(Optional.of(chosen));
        when(offers.findByRequestId(10L)).thenReturn(List.of(chosen, other, alreadyRefused));

        service.accept(100L, 1L);

        assertThat(req.getStatus()).isEqualTo(RequestStatus.AIDE_ACCEPTEE);
        assertThat(chosen.getStatus()).isEqualTo(OfferStatus.ACCEPTEE);
        assertThat(other.getStatus()).isEqualTo(OfferStatus.REFUSEE);
        assertThat(alreadyRefused.getStatus()).isEqualTo(OfferStatus.REFUSEE);
        verify(audit).record(eq(1L), eq(AuditAction.OFFER_ACCEPTED), anyString(), eq(100L), anyString());
    }

    @Test
    @DisplayName("RG-03 : seul l'auteur de la demande peut accepter")
    void onlyAuthorAccepts() {
        HelpRequest req = request(10, author, RequestStatus.OUVERTE);
        when(offers.findRequestIdById(100L)).thenReturn(Optional.of(10L));
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        assertThatThrownBy(() -> service.accept(100L, 2L)).isInstanceOf(ForbiddenOperationException.class);
        assertThat(req.getStatus()).isEqualTo(RequestStatus.OUVERTE);
    }

    @Test
    @DisplayName("RG-03 : impossible d'accepter une seconde aide sur une demande déjà acceptée")
    void cannotAcceptTwice() {
        when(offers.findRequestIdById(100L)).thenReturn(Optional.of(10L));
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, author, RequestStatus.AIDE_ACCEPTEE)));
        assertThatThrownBy(() -> service.accept(100L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("plus ouverte");
    }

    @Test
    @DisplayName("RG-03 : une proposition déjà traitée ne peut pas être refusée")
    void cannotRejectProcessedOffer() {
        HelpRequest req = request(10, author, RequestStatus.OUVERTE);
        when(offers.findRequestIdById(100L)).thenReturn(Optional.of(10L));
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        when(offers.findById(100L)).thenReturn(Optional.of(offer(100, helper, req, OfferStatus.REFUSEE)));
        assertThatThrownBy(() -> service.reject(100L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà été traitée");
    }

    @Test
    @DisplayName("Refuser une proposition en attente la passe en REFUSEE et trace l'action")
    void rejectPendingOffer() {
        HelpRequest req = request(10, author, RequestStatus.OUVERTE);
        HelpOffer pending = offer(100, helper, req, OfferStatus.EN_ATTENTE);
        when(offers.findRequestIdById(100L)).thenReturn(Optional.of(10L));
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        when(offers.findById(100L)).thenReturn(Optional.of(pending));
        service.reject(100L, 1L);
        assertThat(pending.getStatus()).isEqualTo(OfferStatus.REFUSEE);
        assertThat(req.getStatus()).isEqualTo(RequestStatus.OUVERTE);
        verify(audit).record(eq(1L), eq(AuditAction.OFFER_REJECTED), anyString(), eq(100L), anyString());
    }

    @Test
    @DisplayName("RG-11 : l'aidant retire sa proposition en attente")
    void helperWithdrawsPendingOffer() {
        HelpRequest req = request(10, author, RequestStatus.OUVERTE);
        HelpOffer pending = offer(100, helper, req, OfferStatus.EN_ATTENTE);
        when(offers.findRequestIdById(100L)).thenReturn(Optional.of(10L));
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        when(offers.findById(100L)).thenReturn(Optional.of(pending));
        service.withdraw(100L, 2L);
        verify(offers).delete(pending);
        verify(audit).record(eq(2L), eq(AuditAction.OFFER_WITHDRAWN), anyString(), eq(100L), anyString());
    }

    @Test
    @DisplayName("RG-11 : seul l'aidant peut retirer sa proposition")
    void onlyHelperWithdraws() {
        HelpRequest req = request(10, author, RequestStatus.OUVERTE);
        when(offers.findRequestIdById(100L)).thenReturn(Optional.of(10L));
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        when(offers.findById(100L)).thenReturn(Optional.of(offer(100, helper, req, OfferStatus.EN_ATTENTE)));
        assertThatThrownBy(() -> service.withdraw(100L, 1L)).isInstanceOf(ForbiddenOperationException.class);
        verify(offers, never()).delete(any());
    }

    @Test
    @DisplayName("RG-11 : une proposition acceptée ne peut plus être retirée")
    void acceptedOfferCannotBeWithdrawn() {
        HelpRequest req = request(10, author, RequestStatus.AIDE_ACCEPTEE);
        when(offers.findRequestIdById(100L)).thenReturn(Optional.of(10L));
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        when(offers.findById(100L)).thenReturn(Optional.of(offer(100, helper, req, OfferStatus.ACCEPTEE)));
        assertThatThrownBy(() -> service.withdraw(100L, 2L)).isInstanceOf(ConflictException.class);
        verify(audit, never()).record(anyLong(), any(), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Seul l'auteur consulte les propositions de sa demande")
    void onlyAuthorListsOffers() {
        when(requests.findWithDetailsById(10L)).thenReturn(Optional.of(request(10, author, RequestStatus.OUVERTE)));
        assertThatThrownBy(() -> service.listForRequest(10L, 2L)).isInstanceOf(ForbiddenOperationException.class);
    }
}
