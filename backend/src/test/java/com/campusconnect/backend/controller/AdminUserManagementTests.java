package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.support.AbstractApiTest;

/** Gestion des comptes par l'administrateur (RG-10). */
class AdminUserManagementTests extends AbstractApiTest {

    @Test
    void adminCanSearchUsersByEmailAndRole() throws Exception {
        String student = registerAndLogin();
        String email = meEmail(student);

        HttpResponse<String> found = send("GET", "/api/admin/users?q=" + email + "&role=STUDENT", adminLogin(), null);

        assertThat(found.statusCode()).isEqualTo(200);
        assertThat(found.body()).contains(email).contains("\"totalElements\":1").contains("\"enabled\":true");
    }

    @Test
    void adminCanPromoteAStudentToModerator() throws Exception {
        String student = registerAndLogin();
        Long studentId = meId(student);
        String email = meEmail(student);

        HttpResponse<String> changed = send("PUT", "/api/admin/users/" + studentId + "/role", adminLogin(),
                "{\"role\":\"MODERATOR\"}");

        assertThat(changed.statusCode()).isEqualTo(200);
        assertThat(changed.body()).contains("\"role\":\"MODERATOR\"");
        String newToken = loginAs(email);
        assertThat(send("GET", "/api/admin/reports", newToken, null).statusCode()).isEqualTo(200);
    }

    @Test
    void adminCannotChangeOwnRoleOrDisableOwnAccount() throws Exception {
        String admin = adminLogin();
        Long adminId = meId(admin);

        HttpResponse<String> role = send("PUT", "/api/admin/users/" + adminId + "/role", admin, "{\"role\":\"STUDENT\"}");
        HttpResponse<String> disable = send("PUT", "/api/admin/users/" + adminId + "/enabled", admin,
                "{\"enabled\":false}");

        assertThat(role.statusCode()).isEqualTo(409);
        assertThat(role.body()).contains("propre rôle");
        assertThat(disable.statusCode()).isEqualTo(409);
        assertThat(disable.body()).contains("propre compte");
    }

    @Test
    void aDisabledAccountCannotLogInAndItsTokenStopsWorking() throws Exception {
        String student = registerAndLogin();
        Long studentId = meId(student);
        String email = meEmail(student);
        String admin = adminLogin();

        assertThat(send("PUT", "/api/admin/users/" + studentId + "/enabled", admin, "{\"enabled\":false}").statusCode())
                .isEqualTo(200);

        HttpResponse<String> login = tryLogin(email);
        assertThat(login.statusCode()).isEqualTo(403);
        assertThat(login.body()).contains("désactivé");
        HttpResponse<String> withOldToken = send("GET", "/api/users/me", student, null);
        assertThat(withOldToken.statusCode()).isEqualTo(401);
        assertThat(withOldToken.body()).contains("désactivé");

        assertThat(send("PUT", "/api/admin/users/" + studentId + "/enabled", admin, "{\"enabled\":true}").statusCode())
                .isEqualTo(200);
        assertThat(tryLogin(email).statusCode()).isEqualTo(200);
    }

    @Test
    void invalidOrUnknownTargetsAreRejected() throws Exception {
        String admin = adminLogin();

        assertThat(send("PUT", "/api/admin/users/999999999/role", admin, "{\"role\":\"ADMIN\"}").statusCode())
                .isEqualTo(404);
        assertThat(send("PUT", "/api/admin/users/999999999/role", admin, "{}").statusCode()).isEqualTo(400);
        assertThat(send("PUT", "/api/admin/users/999999999/role", admin, "{\"role\":\"ROOT\"}").statusCode())
                .isEqualTo(400);
    }

    @Test
    void studentsCannotManageAccounts() throws Exception {
        String student = registerAndLogin();

        assertThat(send("GET", "/api/admin/users", student, null).statusCode()).isEqualTo(403);
        assertThat(send("PUT", "/api/admin/users/" + meId(student) + "/role", student, "{\"role\":\"ADMIN\"}")
                .statusCode()).isEqualTo(403);
    }
}
