package com.campusconnect.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.AuditLog;
import com.campusconnect.backend.support.AbstractApiTest;

/** Journal d'audit des actions sensibles : qui, quoi, quand. */
class AuditLogTests extends AbstractApiTest {

    private List<AuditAction> actionsOf(Long userId) {
        return auditLogRepository.findByActorIdOrderByIdDesc(userId).stream().map(AuditLog::getAction).toList();
    }

    @Test
    void registrationAndLoginsAreRecorded() throws Exception {
        String student = registerAndLogin();
        Long id = meId(student);
        String email = meEmail(student);
        send("POST", "/api/auth/login", null, "{\"email\":\"" + email + "\",\"password\":\"Mauvais123\"}");

        assertThat(actionsOf(id)).contains(
                AuditAction.USER_REGISTERED, AuditAction.LOGIN_SUCCEEDED, AuditAction.LOGIN_FAILED);
        AuditLog failed = auditLogRepository.findByActorIdOrderByIdDesc(id).get(0);
        assertThat(failed.getAction()).isEqualTo(AuditAction.LOGIN_FAILED);
        assertThat(failed.getActorEmail()).isEqualTo(email);
        assertThat(failed.getCreatedAt()).isNotNull();
    }

    @Test
    void businessActionsAreRecordedWithTheirTarget() throws Exception {
        String author = registerAndLogin();
        String helper = registerAndLogin();
        Long requestId = completedRequest(author, helper);
        assertThat(send("POST", "/api/requests/" + requestId + "/ratings", author, "{\"score\":5}").statusCode())
                .isEqualTo(201);

        assertThat(actionsOf(meId(author))).contains(
                AuditAction.OFFER_ACCEPTED, AuditAction.SESSION_COMPLETED, AuditAction.RATING_SUBMITTED);
        assertThat(auditLogRepository.findByTargetTypeAndTargetIdOrderByIdDesc("HELP_REQUEST", requestId))
                .extracting(AuditLog::getAction)
                .contains(AuditAction.SESSION_COMPLETED, AuditAction.RATING_SUBMITTED);
    }

    @Test
    void moderationAndAdministrationAreRecorded() throws Exception {
        String author = registerAndLogin();
        Long requestId = createRequest(author, newMarker(), subjectIds(author).get(0), "L1");
        String moderator = moderatorLogin();
        assertThat(send("POST", "/api/admin/requests/" + requestId + "/hide", moderator, null).statusCode())
                .isEqualTo(200);
        String admin = adminLogin();
        createSubject(admin, "Audit " + newMarker().substring(9, 17));

        assertThat(actionsOf(meId(moderator))).contains(AuditAction.REQUEST_HIDDEN);
        assertThat(actionsOf(meId(admin))).contains(AuditAction.SUBJECT_CREATED);
    }

    @Test
    void adminCanFilterTheAuditLog() throws Exception {
        String student = registerAndLogin();
        String email = meEmail(student);
        String today = LocalDate.now().toString();

        HttpResponse<String> response = send("GET",
                "/api/admin/audit?action=USER_REGISTERED&email=" + email + "&from=" + today + "&to=" + today,
                adminLogin(), null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"totalElements\":1").contains("USER_REGISTERED").contains(email);
    }

    @Test
    void theAuditLogIsReservedToAdmins() throws Exception {
        assertThat(send("GET", "/api/admin/audit", registerAndLogin(), null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/admin/audit", moderatorLogin(), null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/admin/audit", null, null).statusCode()).isEqualTo(401);
    }
}
