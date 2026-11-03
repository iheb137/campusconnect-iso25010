package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/** Retrait d'une proposition par son auteur (RG-11). */
class OfferWithdrawalTests extends AbstractApiTest {

    private HttpResponse<String> withdraw(String token, Long offerId) throws Exception {
        return send("DELETE", "/api/offers/" + offerId, token, null);
    }

    @Test
    void helperCanWithdrawAPendingOfferAndOfferAgainLater() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L1");
        String helper = registerAndLogin();
        Long offerId = createOffer(helper, requestId);

        assertThat(withdraw(helper, offerId).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/api/requests/" + requestId + "/offers", author, null).body()).isEqualTo("[]");
        assertThat(createOffer(helper, requestId)).isNotNull();
    }

    @Test
    void onlyTheHelperCanWithdraw() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L1");
        Long offerId = createOffer(registerAndLogin(), requestId);

        assertThat(withdraw(author, offerId).statusCode()).isEqualTo(403);
        assertThat(withdraw(registerAndLogin(), offerId).statusCode()).isEqualTo(403);
    }

    @Test
    void anAcceptedOfferCannotBeWithdrawn() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L1");
        String helper = registerAndLogin();
        Long offerId = createOffer(helper, requestId);
        assertThat(send("POST", "/api/offers/" + offerId + "/accept", author, null).statusCode()).isEqualTo(200);

        HttpResponse<String> response = withdraw(helper, offerId);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("en attente");
    }

    @Test
    void unknownOfferReturns404() throws Exception {
        assertThat(withdraw(registerAndLogin(), 999999999L).statusCode()).isEqualTo(404);
    }
}
