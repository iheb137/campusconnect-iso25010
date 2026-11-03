package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/** Tableau de bord administrateur : indicateurs agrégés et filtres (période, matière). */
class AdminStatsTests extends AbstractApiTest {

    @Test
    void statsAreFilteredBySubjectAndPeriod() throws Exception {
        String admin = adminLogin();
        Long subjectId = createSubject(admin, "Stats " + newMarker().substring(9, 17));
        String author = registerAndLogin();
        String helper = registerAndLogin();
        createRequest(author, newMarker(), subjectId, "L1");
        Long done = createRequest(author, newMarker(), subjectId, "L2");
        Long offerId = createOffer(helper, done);
        send("POST", "/api/offers/" + offerId + "/accept", author, null);
        send("POST", "/api/requests/" + done + "/complete", author, null);
        send("POST", "/api/requests/" + done + "/ratings", author, "{\"score\":4}");
        String today = LocalDate.now().toString();

        HttpResponse<String> response = send("GET",
                "/api/admin/stats?subjectId=" + subjectId + "&from=" + today + "&to=" + today, admin, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"totalRequests\":2")
                .contains("\"completedSessions\":1")
                .contains("\"completionRate\":50.0")
                .contains("\"averageRating\":4.0")
                .contains("\"OUVERTE\":1")
                .contains("\"TERMINEE\":1")
                .contains("\"requestsPerDay\":[{\"day\":\"" + today + "\",\"count\":2}]");
    }

    @Test
    void defaultPeriodIsTheLast30Days() throws Exception {
        HttpResponse<String> response = send("GET", "/api/admin/stats", adminLogin(), null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"to\":\"" + LocalDate.now() + "\"")
                .contains("\"from\":\"" + LocalDate.now().minusDays(29) + "\"");
    }

    @Test
    void invalidPeriodIsRejected() throws Exception {
        String admin = adminLogin();

        assertThat(send("GET", "/api/admin/stats?from=2026-05-10&to=2026-05-01", admin, null).statusCode())
                .isEqualTo(400);
        assertThat(send("GET", "/api/admin/stats?from=2020-01-01&to=2026-01-01", admin, null).statusCode())
                .isEqualTo(400);
        assertThat(send("GET", "/api/admin/stats?from=pas-une-date", admin, null).statusCode()).isEqualTo(400);
    }

    @Test
    void statsAreReservedToAdmins() throws Exception {
        assertThat(send("GET", "/api/admin/stats", registerAndLogin(), null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/admin/stats", moderatorLogin(), null).statusCode()).isEqualTo(403);
    }
}
