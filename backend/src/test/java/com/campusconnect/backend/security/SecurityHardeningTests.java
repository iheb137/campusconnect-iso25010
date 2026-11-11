package com.campusconnect.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import com.campusconnect.backend.support.AbstractApiTest;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Sécurité (ISO 25010 — confidentialité, intégrité, authenticité, non-répudiation).
 * Chaque test correspond à une exigence SEC-xx de la matrice de traçabilité.
 */
@DisplayName("Sécurité — durcissement de l'API")
class SecurityHardeningTests extends AbstractApiTest {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String signedToken(String secret, String subject, String role, Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("role", role)
                .issueTime(new Date())
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    @Test
    @DisplayName("SEC-01 : sans jeton, les routes protégées répondent 401")
    void anonymousIsRejected() throws Exception {
        assertThat(send("GET", "/api/users/me", null, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/admin/users", null, null).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-02 : un jeton dont la charge utile a été modifiée (rôle ADMIN forgé) est refusé")
    void tamperedTokenIsRejected() throws Exception {
        String token = registerAndLogin();
        String[] parts = token.split("\\.");
        String forgedPayload = b64("{\"sub\":\"1\",\"role\":\"ADMIN\",\"exp\":4102444800}");
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];
        assertThat(send("GET", "/api/admin/users", forged, null).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-03 : un jeton non signé (alg=none) est refusé")
    void unsignedTokenIsRejected() throws Exception {
        String none = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "."
                + b64("{\"sub\":\"1\",\"role\":\"ADMIN\",\"exp\":4102444800}") + ".";
        assertThat(send("GET", "/api/admin/users", none, null).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-04 : un jeton signé avec une autre clé est refusé")
    void tokenSignedWithOtherKeyIsRejected() throws Exception {
        String token = signedToken("une-autre-cle-secrete-de-32-caracteres-minimum!", "1", "ADMIN",
                Instant.now().plusSeconds(600));
        assertThat(send("GET", "/api/admin/users", token, null).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-05 : un jeton expiré est refusé")
    void expiredTokenIsRejected() throws Exception {
        String token = registerAndLogin();
        String subject = String.valueOf(meId(token));
        String expired = signedToken(jwtSecret, subject, "STUDENT", Instant.now().minusSeconds(3600));
        assertThat(send("GET", "/api/users/me", expired, null).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("SEC-06 : un étudiant n'accède à aucune route d'administration ni de modération (403)")
    void studentCannotReachAdminRoutes() throws Exception {
        String student = registerAndLogin();
        for (String path : new String[] {"/api/admin/users", "/api/admin/subjects", "/api/admin/audit",
                "/api/admin/stats", "/api/admin/reports"}) {
            int status = path.equals("/api/admin/subjects")
                    ? send("POST", path, student, "{\"name\":\"X\"}").statusCode()
                    : send("GET", path, student, null).statusCode();
            assertThat(status).as(path).isEqualTo(403);
        }
    }

    @Test
    @DisplayName("SEC-07 : le hachage du mot de passe n'apparaît dans aucune réponse")
    void passwordHashNeverExposed() throws Exception {
        String token = registerAndLogin();
        String me = send("GET", "/api/users/me", token, null).body();
        String admin = send("GET", "/api/admin/users?size=50", adminLogin(), null).body();
        for (String body : new String[] {me, admin}) {
            assertThat(body).doesNotContainIgnoringCase("password").doesNotContain("$2a$").doesNotContain("$2b$");
        }
    }

    @Test
    @DisplayName("SEC-08 : en-têtes de sécurité présents sur les réponses de l'API")
    void securityHeadersArePresent() throws Exception {
        HttpResponse<String> response = send("GET", "/api/health", null, null);
        assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
        assertThat(response.headers().firstValue("X-Frame-Options")).hasValue("DENY");
        assertThat(response.headers().firstValue("Content-Security-Policy"))
                .hasValueSatisfying(csp -> assertThat(csp).contains("default-src 'none'").contains("frame-ancestors 'none'"));
        assertThat(response.headers().firstValue("Referrer-Policy")).hasValue("no-referrer");
        assertThat(response.headers().firstValue("Cache-Control")).isPresent();
    }

    @Test
    @DisplayName("SEC-09 : les erreurs ne divulguent ni trace technique ni détail interne")
    void errorsDoNotLeakInternals() throws Exception {
        String token = registerAndLogin();
        HttpResponse<String> notFound = send("GET", "/api/requests/abc", token, null);
        assertThat(notFound.statusCode()).isEqualTo(400);
        HttpResponse<String> wrongMethod = send("PATCH", "/api/health", null, null);
        assertThat(wrongMethod.statusCode()).isIn(401, 405);
        HttpResponse<String> unknown = send("GET", "/api/inexistant", token, null);
        assertThat(unknown.statusCode()).isEqualTo(404);
        for (HttpResponse<String> response : java.util.List.of(notFound, wrongMethod, unknown)) {
            assertThat(response.body()).doesNotContain("Exception").doesNotContain("at com.").doesNotContain("trace");
        }
    }

    @Test
    @DisplayName("SEC-10 (RG-13) : après 5 échecs, le compte est bloqué (429) même avec le bon mot de passe")
    void bruteForceIsBlocked() throws Exception {
        String token = registerAndLogin();
        String email = meEmail(token);
        String wrong = "{\"email\":\"" + email + "\",\"password\":\"mauvais-mot-de-passe\"}";
        for (int i = 0; i < 5; i++) {
            assertThat(send("POST", "/api/auth/login", null, wrong).statusCode()).isEqualTo(401);
        }
        HttpResponse<String> blocked = tryLogin(email);
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(blocked.headers().firstValue("Retry-After")).isPresent();
        assertThat(blocked.body()).contains("Trop de tentatives");
    }

    @Test
    @DisplayName("SEC-11 : une injection SQL dans la recherche est traitée comme du texte")
    void sqlInjectionInSearchIsHarmless() throws Exception {
        String token = registerAndLogin();
        String payload = java.net.URLEncoder.encode("' OR '1'='1'; DROP TABLE users; --", StandardCharsets.UTF_8);
        HttpResponse<String> response = send("GET", "/api/requests?q=" + payload, token, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"totalElements\":0");
        assertThat(send("GET", "/api/users/me", token, null).statusCode()).isEqualTo(200);
    }
}
