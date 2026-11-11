package com.campusconnect.backend.service;

import static com.campusconnect.backend.service.Fixtures.offer;
import static com.campusconnect.backend.service.Fixtures.request;
import static com.campusconnect.backend.service.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.campusconnect.backend.dto.RatingInput;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.Rating;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.RatingRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitaire — SessionService (RG-03, RG-05)")
class SessionServiceUnitTest {

    @Mock HelpRequestRepository requests;
    @Mock HelpOfferRepository offers;
    @Mock RatingRepository ratings;
    @Mock AuditService audit;
    @InjectMocks SessionService service;

    private final User author = student(1);
    private final User helper = student(2);

    @Test
    @DisplayName("RG-03 : l'auteur clôture une séance AIDE_ACCEPTEE → TERMINEE avec date de fin")
    void authorCompletesAcceptedSession() {
        HelpRequest req = request(10, author, RequestStatus.AIDE_ACCEPTEE);
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        service.complete(10L, 1L);
        assertThat(req.getStatus()).isEqualTo(RequestStatus.TERMINEE);
        assertThat(req.getCompletedAt()).isNotNull();
    }

    @ParameterizedTest(name = "RG-03 : transition interdite depuis {0}")
    @EnumSource(value = RequestStatus.class, names = {"OUVERTE", "TERMINEE"})
    void completeRequiresAcceptedHelp(RequestStatus status) {
        HelpRequest req = request(10, author, status);
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(req));
        assertThatThrownBy(() -> service.complete(10L, 1L)).isInstanceOf(ConflictException.class);
        assertThat(req.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("RG-03 : seul l'auteur peut clôturer")
    void onlyAuthorCompletes() {
        when(requests.findByIdForUpdate(10L)).thenReturn(Optional.of(request(10, author, RequestStatus.AIDE_ACCEPTEE)));
        assertThatThrownBy(() -> service.complete(10L, 2L)).isInstanceOf(ForbiddenOperationException.class);
    }

    private HelpRequest finishedSession() {
        HelpRequest req = request(10, author, RequestStatus.TERMINEE);
        when(requests.findWithDetailsById(10L)).thenReturn(Optional.of(req));
        when(offers.findFirstByRequestIdAndStatus(10L, OfferStatus.ACCEPTEE))
                .thenReturn(Optional.of(offer(100, helper, req, OfferStatus.ACCEPTEE)));
        return req;
    }

    @Test
    @DisplayName("RG-05 : l'aidant évalue l'auteur après la clôture")
    void helperRatesAuthor() {
        finishedSession();
        when(ratings.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        service.rate(10L, 2L, new RatingInput(4, "  Très clair  "));
        var captor = org.mockito.ArgumentCaptor.forClass(Rating.class);
        verify(ratings).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRater()).isSameAs(helper);
        assertThat(captor.getValue().getRated()).isSameAs(author);
        assertThat(captor.getValue().getComment()).isEqualTo("Très clair");
    }

    @Test
    @DisplayName("RG-05 : un commentaire vide est enregistré comme absent")
    void blankCommentIsNull() {
        finishedSession();
        when(ratings.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        service.rate(10L, 1L, new RatingInput(5, "   "));
        var captor = org.mockito.ArgumentCaptor.forClass(Rating.class);
        verify(ratings).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getComment()).isNull();
        assertThat(captor.getValue().getRated()).isSameAs(helper);
    }

    @Test
    @DisplayName("RG-05 : un non-participant ne peut pas évaluer")
    void outsiderCannotRate() {
        finishedSession();
        assertThatThrownBy(() -> service.rate(10L, 3L, new RatingInput(5, null)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("RG-05 : pas d'évaluation avant la clôture")
    void noRatingBeforeCompletion() {
        HelpRequest req = request(10, author, RequestStatus.AIDE_ACCEPTEE);
        when(requests.findWithDetailsById(10L)).thenReturn(Optional.of(req));
        when(offers.findFirstByRequestIdAndStatus(10L, OfferStatus.ACCEPTEE))
                .thenReturn(Optional.of(offer(100, helper, req, OfferStatus.ACCEPTEE)));
        assertThatThrownBy(() -> service.rate(10L, 1L, new RatingInput(5, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("après la clôture");
    }

    @Test
    @DisplayName("RG-05 : une seule évaluation par participant")
    void ratingOnlyOnce() {
        finishedSession();
        when(ratings.existsByRequestIdAndRaterId(10L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.rate(10L, 1L, new RatingInput(5, null)))
                .isInstanceOf(ConflictException.class);
        verify(ratings, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("RG-05 : un non-participant ne voit pas les évaluations")
    void outsiderCannotSeeRatings() {
        finishedSession();
        assertThatThrownBy(() -> service.ratings(10L, 3L)).isInstanceOf(ForbiddenOperationException.class);
    }
}
