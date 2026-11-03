package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/** Suppression d'une demande par son auteur (RG-02). */
class RequestDeletionTests extends AbstractApiTest {

    private HttpResponse<String> delete(String token, Long requestId) throws Exception {
        return send("DELETE", "/api/requests/" + requestId, token, null);
    }

    @Test
    void authorCanDeleteAnOpenRequestWithItsOffersAndReports() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L2");
        createOffer(registerAndLogin(), requestId);
        assertThat(send("POST", "/api/requests/" + requestId + "/reports", registerAndLogin(),
                "{\"reason\":\"Doublon d'une autre demande\"}").statusCode()).isEqualTo(201);

        assertThat(delete(author, requestId).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/api/requests/" + requestId, author, null).statusCode()).isEqualTo(404);
        assertThat(helpOfferRepository.findByRequestId(requestId)).isEmpty();
        assertThat(reportRepository.findByRequestId(requestId)).isEmpty();
    }

    @Test
    void onlyTheAuthorCanDelete() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L2");

        HttpResponse<String> byOther = delete(registerAndLogin(), requestId);

        assertThat(byOther.statusCode()).isEqualTo(403);
        assertThat(byOther.body()).contains("auteur");
        assertThat(delete(null, requestId).statusCode()).isEqualTo(401);
    }

    @Test
    void aRequestThatIsNoLongerOpenCannotBeDeleted() throws Exception {
        String author = registerAndLogin();
        Long requestId = acceptedRequest(author, registerAndLogin());

        HttpResponse<String> response = delete(author, requestId);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("ouverte");
    }

    @Test
    void unknownRequestReturns404() throws Exception {
        assertThat(delete(registerAndLogin(), 999999999L).statusCode()).isEqualTo(404);
    }
}
