package com.campusconnect.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.AuditLogRepository;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.RatingRepository;
import com.campusconnect.backend.repository.ReportRepository;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Outils communs aux tests qui appellent l'API par HTTP.
 * Tout ce qui est créé par les tests est supprimé après chaque test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractApiTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected HelpRequestRepository helpRequestRepository;

    @Autowired
    protected HelpOfferRepository helpOfferRepository;

    @Autowired
    protected RatingRepository ratingRepository;

    @Autowired
    protected ReportRepository reportRepository;

    @Autowired
    protected SubjectRepository subjectRepository;

    @Autowired
    protected AuditLogRepository auditLogRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<String> createdEmails = new ArrayList<>();
    private final List<String> createdSubjects = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedData() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent(user -> {
                ratingRepository.deleteAll(ratingRepository.findByRaterIdOrRatedId(user.getId(), user.getId()));
                reportRepository.deleteAll(reportRepository.findByReporterId(user.getId()));
                helpOfferRepository.deleteAll(helpOfferRepository.findByHelperId(user.getId()));
                List<HelpRequest> requests = helpRequestRepository.findByAuthorId(user.getId());
                for (HelpRequest request : requests) {
                    ratingRepository.deleteAll(ratingRepository.findByRequestId(request.getId()));
                    reportRepository.deleteAll(reportRepository.findByRequestId(request.getId()));
                    helpOfferRepository.deleteAll(helpOfferRepository.findByRequestId(request.getId()));
                }
                helpRequestRepository.deleteAll(requests);
                userRepository.delete(user);
            });
        }
        createdEmails.clear();
        for (String name : createdSubjects) {
            subjectRepository.findByName(name).ifPresent(subjectRepository::delete);
        }
        createdSubjects.clear();
    }

    protected HttpResponse<String> send(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (json != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Crée un étudiant, le connecte et renvoie son jeton. */
    protected String registerAndLogin() throws Exception {
        String email = newEmail();

        HttpResponse<String> registration = send("POST", "/api/auth/register", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\",\"fullName\":\"Etudiant Test\"}");
        assertThat(registration.statusCode()).isEqualTo(201);
        return login(email);
    }

    /** Crée un administrateur directement en base, le connecte et renvoie son jeton. */
    protected String adminLogin() throws Exception {
        return loginWithRole(Role.ADMIN, "Admin Test");
    }

    /** Crée un modérateur directement en base, le connecte et renvoie son jeton. */
    protected String moderatorLogin() throws Exception {
        return loginWithRole(Role.MODERATOR, "Moderateur Test");
    }

    private String loginWithRole(Role role, String fullName) throws Exception {
        String email = newEmail();
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode("MotDePasse123"));
        user.setRole(role);
        userRepository.save(user);
        return login(email);
    }

    /** Identifiant de l'utilisateur connecté (lu via /api/users/me). */
    protected Long meId(String token) throws Exception {
        HttpResponse<String> me = send("GET", "/api/users/me", token, null);
        assertThat(me.statusCode()).isEqualTo(200);
        return firstId(me.body());
    }

    /** Email de l'utilisateur connecté. */
    protected String meEmail(String token) throws Exception {
        String body = send("GET", "/api/users/me", token, null).body();
        Matcher matcher = Pattern.compile("\"email\":\"([^\"]+)\"").matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    /** Crée une matière via l'API d'administration (supprimée après le test) et renvoie son identifiant. */
    protected Long createSubject(String adminToken, String name) throws Exception {
        createdSubjects.add(name);
        HttpResponse<String> response = send("POST", "/api/admin/subjects", adminToken, "{\"name\":\"" + name + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return firstId(response.body());
    }

    /** Enregistre un nom de matière à supprimer après le test (si un test la crée lui-même). */
    protected void trackSubject(String name) {
        createdSubjects.add(name);
    }

    /** Connexion avec un email et le mot de passe de test ; renvoie la réponse brute. */
    protected HttpResponse<String> tryLogin(String email) throws Exception {
        return send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\"}");
    }

    protected String loginAs(String email) throws Exception {
        return login(email);
    }

    private String newEmail() {
        String email = "test." + UUID.randomUUID() + "@campusconnect.local";
        createdEmails.add(email);
        return email;
    }

    private String login(String email) throws Exception {
        HttpResponse<String> login = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"MotDePasse123\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        Matcher matcher = TOKEN_PATTERN.matcher(login.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    protected Long firstId(String body) {
        Matcher matcher = ID_PATTERN.matcher(body);
        assertThat(matcher.find()).isTrue();
        return Long.valueOf(matcher.group(1));
    }

    protected List<Long> subjectIds(String token) throws Exception {
        HttpResponse<String> response = send("GET", "/api/subjects", token, null);
        assertThat(response.statusCode()).isEqualTo(200);
        List<Long> ids = new ArrayList<>();
        Matcher matcher = ID_PATTERN.matcher(response.body());
        while (matcher.find()) {
            ids.add(Long.valueOf(matcher.group(1)));
        }
        assertThat(ids).hasSizeGreaterThanOrEqualTo(2);
        return ids;
    }

    protected String newMarker() {
        return "Marqueur-" + UUID.randomUUID();
    }

    /** Crée une demande dont le titre contient le marqueur et renvoie son identifiant. */
    protected Long createRequest(String token, String marker, Long subjectId, String level) throws Exception {
        HttpResponse<String> response = send("POST", "/api/requests", token,
                "{\"title\":\"Demande " + marker + "\","
                        + "\"description\":\"Description de test suffisamment longue.\","
                        + "\"availability\":\"Tous les jours\","
                        + "\"subjectId\":" + subjectId + ",\"level\":\"" + level + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return firstId(response.body());
    }

    /** Fait proposer son aide à un étudiant et renvoie l'identifiant de la proposition. */
    protected Long createOffer(String helperToken, Long requestId) throws Exception {
        HttpResponse<String> response = send("POST", "/api/requests/" + requestId + "/offers", helperToken,
                "{\"message\":\"Je peux aider volontiers\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return firstId(response.body());
    }

    /** Publie une demande, fait accepter l'aide de {@code helperToken} et renvoie l'identifiant de la demande. */
    protected Long acceptedRequest(String authorToken, String helperToken) throws Exception {
        Long requestId = createRequest(authorToken, newMarker(), subjectIds(authorToken).get(0), "L2");
        Long offerId = createOffer(helperToken, requestId);
        assertThat(send("POST", "/api/offers/" + offerId + "/accept", authorToken, null).statusCode())
                .isEqualTo(200);
        return requestId;
    }

    /** Comme {@link #acceptedRequest}, puis clôture la séance. */
    protected Long completedRequest(String authorToken, String helperToken) throws Exception {
        Long requestId = acceptedRequest(authorToken, helperToken);
        assertThat(send("POST", "/api/requests/" + requestId + "/complete", authorToken, null).statusCode())
                .isEqualTo(200);
        return requestId;
    }
}
