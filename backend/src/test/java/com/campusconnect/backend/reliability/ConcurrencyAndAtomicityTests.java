package com.campusconnect.backend.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;

import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.service.AuditService;
import com.campusconnect.backend.support.AbstractApiTest;

/**
 * Fiabilité (ISO 25010 — maturité, tolérance aux fautes) : accès concurrents et atomicité des transactions.
 */
@DisplayName("Fiabilité — concurrence et atomicité")
class ConcurrencyAndAtomicityTests extends AbstractApiTest {

    @MockitoSpyBean
    private AuditService auditService;

    @AfterEach
    void resetSpy() {
        reset((AuditService) AopTestUtils.getUltimateTargetObject(auditService));
    }

    /** Lance toutes les tâches au même instant et renvoie les codes HTTP obtenus. */
    private List<Integer> simultaneously(List<Callable<HttpResponse<String>>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<HttpResponse<String>>> futures = new ArrayList<>();
            for (Callable<HttpResponse<String>> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<HttpResponse<String>> future : futures) {
                statuses.add(future.get().statusCode());
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("REL-01 : deux acceptations simultanées sur la même demande → une seule réussit (verrou pessimiste)")
    void concurrentAcceptsKeepOneHelper() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L2");
        Long offerA = createOffer(registerAndLogin(), requestId);
        Long offerB = createOffer(registerAndLogin(), requestId);

        List<Integer> statuses = simultaneously(List.of(
                () -> send("POST", "/api/offers/" + offerA + "/accept", author, null),
                () -> send("POST", "/api/offers/" + offerB + "/accept", author, null)));

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(helpRequestRepository.findById(requestId).orElseThrow().getStatus())
                .isEqualTo(RequestStatus.AIDE_ACCEPTEE);
        assertThat(helpOfferRepository.findByRequestId(requestId))
                .extracting(offer -> offer.getStatus())
                .containsExactlyInAnyOrder(OfferStatus.ACCEPTEE, OfferStatus.REFUSEE);
    }

    @Test
    @DisplayName("REL-02 : huit propositions simultanées du même aidant → une seule est enregistrée")
    void concurrentDuplicateOffersAreRejected() throws Exception {
        String author = registerAndLogin();
        String helper = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L2");

        List<Callable<HttpResponse<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> send("POST", "/api/requests/" + requestId + "/offers", helper,
                    "{\"message\":\"Je peux aider volontiers\"}"));
        }
        List<Integer> statuses = simultaneously(tasks);

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(7);
        assertThat(helpOfferRepository.findByRequestId(requestId)).hasSize(1);
    }

    @Test
    @DisplayName("REL-03 : une panne pendant l'acceptation annule toute la transaction (aucun état partiel)")
    void failureDuringAcceptRollsBack() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L2");
        Long offerA = createOffer(registerAndLogin(), requestId);
        Long offerB = createOffer(registerAndLogin(), requestId);

        // On configure l'espion lui-même (derrière le proxy transactionnel de Spring)
        AuditService spy = AopTestUtils.getUltimateTargetObject(auditService);
        doThrow(new IllegalStateException("panne simulée du journal d'audit"))
                .when(spy).record(any(), eq(AuditAction.OFFER_ACCEPTED), anyString(), any(), any());

        HttpResponse<String> response = send("POST", "/api/offers/" + offerA + "/accept", author, null);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("Une erreur inattendue est survenue").doesNotContain("panne simulée");
        assertThat(helpRequestRepository.findById(requestId).orElseThrow().getStatus()).isEqualTo(RequestStatus.OUVERTE);
        assertThat(helpOfferRepository.findById(offerA).orElseThrow().getStatus()).isEqualTo(OfferStatus.EN_ATTENTE);
        assertThat(helpOfferRepository.findById(offerB).orElseThrow().getStatus()).isEqualTo(OfferStatus.EN_ATTENTE);

        // Reprise : une fois la panne levée, la même opération réussit.
        reset(spy);
        assertThat(send("POST", "/api/offers/" + offerA + "/accept", author, null).statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("REL-04 : la sonde /api/health vérifie la base de données")
    void healthChecksDatabase() throws Exception {
        HttpResponse<String> response = send("GET", "/api/health", null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"database\":\"up\"");
    }
}
