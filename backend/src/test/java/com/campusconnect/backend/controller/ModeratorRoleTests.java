package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/**
 * Troisième rôle : le modérateur traite les signalements mais n'a pas accès à l'administration
 * (comptes, matières, audit, statistiques) ni aux actions réservées aux étudiants.
 */
class ModeratorRoleTests extends AbstractApiTest {

    @Test
    void moderatorCanListAndModerateReportedRequests() throws Exception {
        String author = registerAndLogin();
        String marker = newMarker();
        Long requestId = createRequest(author, marker, subjectIds(author).get(0), "L2");
        assertThat(send("POST", "/api/requests/" + requestId + "/reports", registerAndLogin(),
                "{\"reason\":\"Publication hors sujet\"}").statusCode()).isEqualTo(201);
        String moderator = moderatorLogin();

        HttpResponse<String> list = send("GET", "/api/admin/reports", moderator, null);
        HttpResponse<String> hide = send("POST", "/api/admin/requests/" + requestId + "/hide", moderator, null);

        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains(marker);
        assertThat(hide.statusCode()).isEqualTo(200);
        assertThat(hide.body()).contains("\"hidden\":true");
        assertThat(send("POST", "/api/admin/requests/" + requestId + "/restore", moderator, null).statusCode())
                .isEqualTo(200);
    }

    @Test
    void moderatorCannotUseAdministrationEndpoints() throws Exception {
        String moderator = moderatorLogin();

        assertThat(send("GET", "/api/admin/users", moderator, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/admin/audit", moderator, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/admin/stats", moderator, null).statusCode()).isEqualTo(403);
        assertThat(send("POST", "/api/admin/subjects", moderator, "{\"name\":\"Interdit\"}").statusCode())
                .isEqualTo(403);
    }

    @Test
    void moderatorCannotActAsAStudent() throws Exception {
        String moderator = moderatorLogin();
        Long subjectId = subjectIds(moderator).get(0);

        HttpResponse<String> publish = send("POST", "/api/requests", moderator,
                "{\"title\":\"Demande modérateur\",\"description\":\"Description suffisamment longue\","
                        + "\"availability\":\"Lundi\",\"subjectId\":" + subjectId + ",\"level\":\"L1\"}");

        assertThat(publish.statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/dashboard", moderator, null).statusCode()).isEqualTo(403);
    }

    @Test
    void loginReturnsTheModeratorRole() throws Exception {
        String moderator = moderatorLogin();

        HttpResponse<String> me = send("GET", "/api/users/me", moderator, null);

        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("\"role\":\"MODERATOR\"");
    }
}
