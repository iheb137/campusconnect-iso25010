package com.campusconnect.backend.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.campusconnect.backend.exception.TooManyAttemptsException;

@DisplayName("Unitaire — LoginAttemptService (RG-13)")
class LoginAttemptServiceUnitTest {

    /** Horloge réglable pour simuler l'écoulement du temps. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-03T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService service = new LoginAttemptService(5, Duration.ofMinutes(15), clock);

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure("a@test.tn");
        }
    }

    @Test
    @DisplayName("RG-13 : 4 échecs ne bloquent pas")
    void fourFailuresAllowed() {
        fail(4);
        assertThatCode(() -> service.checkAllowed("a@test.tn")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RG-13 : au 5e échec, le compte est bloqué 15 minutes")
    void fifthFailureBlocks() {
        fail(5);
        assertThatThrownBy(() -> service.checkAllowed("a@test.tn"))
                .isInstanceOf(TooManyAttemptsException.class)
                .extracting(e -> ((TooManyAttemptsException) e).getRetryAfterSeconds())
                .isEqualTo(900L);
        clock.advance(Duration.ofMinutes(15));
        assertThatCode(() -> service.checkAllowed("a@test.tn")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RG-13 : le blocage ne concerne que l'email visé")
    void otherAccountsUnaffected() {
        fail(5);
        assertThatCode(() -> service.checkAllowed("b@test.tn")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RG-13 : une connexion réussie remet le compteur à zéro")
    void successResetsCounter() {
        fail(4);
        service.recordSuccess("a@test.tn");
        fail(4);
        assertThatCode(() -> service.checkAllowed("a@test.tn")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RG-13 : les échecs anciens (hors fenêtre) ne comptent plus")
    void oldFailuresExpire() {
        fail(4);
        clock.advance(Duration.ofMinutes(16));
        fail(1);
        assertThatCode(() -> service.checkAllowed("a@test.tn")).doesNotThrowAnyException();
    }
}
