package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/** CRUD des matières par l'administrateur (RG-09). */
class SubjectAdminTests extends AbstractApiTest {

    private String uniqueName() {
        return "Matiere " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void adminCanCreateRenameAndDeleteAnUnusedSubject() throws Exception {
        String admin = adminLogin();
        String name = uniqueName();
        Long id = createSubject(admin, name);
        String renamed = uniqueName();
        trackSubject(renamed);

        HttpResponse<String> update = send("PUT", "/api/admin/subjects/" + id, admin, "{\"name\":\"" + renamed + "\"}");
        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/subjects", admin, null).body()).contains(renamed).doesNotContain(name);

        assertThat(send("DELETE", "/api/admin/subjects/" + id, admin, null).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/api/subjects", admin, null).body()).doesNotContain(renamed);
    }

    @Test
    void subjectNamesAreUniqueIgnoringCase() throws Exception {
        String admin = adminLogin();
        String name = uniqueName();
        createSubject(admin, name);

        HttpResponse<String> duplicate = send("POST", "/api/admin/subjects", admin,
                "{\"name\":\"" + name.toUpperCase() + "\"}");

        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("déjà ce nom");
    }

    @Test
    void aSubjectUsedByARequestCannotBeDeleted() throws Exception {
        String admin = adminLogin();
        Long id = createSubject(admin, uniqueName());
        String student = registerAndLogin();
        createRequest(student, newMarker(), id, "L1");

        HttpResponse<String> delete = send("DELETE", "/api/admin/subjects/" + id, admin, null);

        assertThat(delete.statusCode()).isEqualTo(409);
        assertThat(delete.body()).contains("utilisée");
    }

    @Test
    void invalidInputUnknownSubjectAndAccessAreHandled() throws Exception {
        String admin = adminLogin();

        assertThat(send("POST", "/api/admin/subjects", admin, "{\"name\":\"x\"}").statusCode()).isEqualTo(400);
        assertThat(send("PUT", "/api/admin/subjects/999999999", admin, "{\"name\":\"Inconnue\"}").statusCode())
                .isEqualTo(404);
        assertThat(send("DELETE", "/api/admin/subjects/999999999", admin, null).statusCode()).isEqualTo(404);
        assertThat(send("POST", "/api/admin/subjects", registerAndLogin(), "{\"name\":\"Interdite\"}").statusCode())
                .isEqualTo(403);
    }
}
