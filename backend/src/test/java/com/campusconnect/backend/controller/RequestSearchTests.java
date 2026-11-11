package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

@DisplayName("Recherche plein texte sur les demandes")
class RequestSearchTests extends AbstractApiTest {

    private String q(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("FUN-SEARCH-01 : la recherche trouve le titre sans tenir compte de la casse")
    void searchIsCaseInsensitive() throws Exception {
        String token = registerAndLogin();
        String marker = newMarker();
        createRequest(token, marker, subjectIds(token).get(0), "L2");
        String body = send("GET", "/api/requests?q=" + q(marker.toUpperCase()), token, null).body();
        assertThat(body).contains("\"totalElements\":1").contains(marker);
    }

    @Test
    @DisplayName("FUN-SEARCH-02 : la recherche se combine aux filtres (matière)")
    void searchCombinesWithFilters() throws Exception {
        String token = registerAndLogin();
        String marker = newMarker();
        var subjects = subjectIds(token);
        createRequest(token, marker, subjects.get(0), "L2");
        createRequest(token, marker, subjects.get(1), "L2");
        assertThat(send("GET", "/api/requests?q=" + q(marker), token, null).body()).contains("\"totalElements\":2");
        assertThat(send("GET", "/api/requests?q=" + q(marker) + "&subjectId=" + subjects.get(1), token, null).body())
                .contains("\"totalElements\":1");
    }

    @Test
    @DisplayName("FUN-SEARCH-03 : les jokers SQL (% et _) sont cherchés littéralement")
    void wildcardsAreLiteral() throws Exception {
        String token = registerAndLogin();
        createRequest(token, newMarker(), subjectIds(token).get(0), "L2");
        assertThat(send("GET", "/api/requests?q=" + q("%"), token, null).body()).contains("\"totalElements\":0");
        assertThat(send("GET", "/api/requests?q=" + q("Marqueur_"), token, null).body()).contains("\"totalElements\":0");
    }
}
