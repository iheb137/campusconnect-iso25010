package com.campusconnect.backend.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.Rating;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.RatingRepository;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Génère un jeu de données réaliste et reproductible (graine fixe) pour la démonstration,
 * la recherche « à fort volume » et les tests de performance.
 * Désactivé par défaut : activer avec DEMO_REQUESTS=2000 (par exemple). Ne s'exécute qu'une fois
 * (présence du compte demo.student.1@campusconnect.local).
 * Comptes créés : demo.student.N@campusconnect.local, mot de passe {@value #DEMO_PASSWORD}.
 */
@Component
@Order(10)
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Demo12345";
    static final String EMAIL_PREFIX = "demo.student.";
    private static final String EMAIL_DOMAIN = "@campusconnect.local";
    private static final int STUDENTS = 40;
    private static final int BATCH = 200;

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String[] TOPICS = {
        "Exercices sur", "Révision de", "Projet en", "Préparation d'examen :", "Questions sur", "TP de" };
    private static final String[] AVAILABILITIES = {
        "Lundi soir, en ligne", "Mardi après 17 h", "Mercredi après-midi, à la bibliothèque",
        "Jeudi midi", "Week-end, en ligne", "Vendredi matin" };

    private final int requestCount;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final HelpOfferRepository helpOfferRepository;
    private final RatingRepository ratingRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactions;

    public DemoDataSeeder(
            @Value("${app.demo.requests:0}") int requestCount,
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            HelpRequestRepository helpRequestRepository,
            HelpOfferRepository helpOfferRepository,
            RatingRepository ratingRepository,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactions) {
        this.requestCount = requestCount;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.helpRequestRepository = helpRequestRepository;
        this.helpOfferRepository = helpOfferRepository;
        this.ratingRepository = ratingRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactions = transactions;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (requestCount <= 0) {
            return;
        }
        if (userRepository.existsByEmail(EMAIL_PREFIX + 1 + EMAIL_DOMAIN)) {
            log.info("Données de démonstration déjà présentes");
            return;
        }
        long started = System.currentTimeMillis();
        Random random = new Random(42);
        List<User> students = transactions.execute(status -> createStudents());
        List<Subject> subjects = subjectRepository.findAll();
        for (int done = 0; done < requestCount; done += BATCH) {
            int size = Math.min(BATCH, requestCount - done);
            transactions.executeWithoutResult(status -> createRequests(size, students, subjects, random));
        }
        log.info("Données de démonstration : {} étudiants, {} demandes en {} ms",
                STUDENTS, requestCount, System.currentTimeMillis() - started);
    }

    private List<User> createStudents() {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        Level[] levels = Level.values();
        List<User> students = new ArrayList<>();
        for (int i = 1; i <= STUDENTS; i++) {
            User user = new User();
            user.setEmail(EMAIL_PREFIX + i + EMAIL_DOMAIN);
            user.setFullName("Étudiant démo " + i);
            user.setPasswordHash(hash);
            user.setRole(Role.STUDENT);
            user.setLevel(levels[i % levels.length]);
            students.add(user);
        }
        return userRepository.saveAll(students);
    }

    private void createRequests(int count, List<User> students, List<Subject> subjects, Random random) {
        Instant now = Instant.now();
        Level[] levels = Level.values();
        for (int i = 0; i < count; i++) {
            User author = students.get(random.nextInt(students.size()));
            Subject subject = subjects.get(random.nextInt(subjects.size()));
            Instant created = now.minus(random.nextInt(90 * 24), ChronoUnit.HOURS);

            HelpRequest request = new HelpRequest();
            request.setTitle(TOPICS[random.nextInt(TOPICS.length)] + " " + subject.getName());
            request.setDescription("Demande générée pour la démonstration : besoin d'explications et d'exercices corrigés en "
                    + subject.getName() + ".");
            request.setAvailability(AVAILABILITIES[random.nextInt(AVAILABILITIES.length)]);
            request.setLevel(levels[random.nextInt(levels.length)]);
            request.setSubject(subject);
            request.setAuthor(author);
            request.setCreatedAt(created);

            int roll = random.nextInt(100);
            RequestStatus status = roll < 50 ? RequestStatus.OUVERTE
                    : roll < 75 ? RequestStatus.AIDE_ACCEPTEE : RequestStatus.TERMINEE;
            request.setStatus(status);
            if (status == RequestStatus.TERMINEE) {
                request.setCompletedAt(created.plus(1 + random.nextInt(72), ChronoUnit.HOURS));
            }
            helpRequestRepository.save(request);

            List<User> helpers = pickHelpers(students, author, random.nextInt(4) + (status == RequestStatus.OUVERTE ? 0 : 1), random);
            User accepted = null;
            for (int h = 0; h < helpers.size(); h++) {
                HelpOffer offer = new HelpOffer();
                offer.setHelper(helpers.get(h));
                offer.setRequest(request);
                offer.setMessage("Je peux vous aider sur ce sujet, contactez-moi.");
                offer.setCreatedAt(created.plus(1 + h, ChronoUnit.HOURS));
                if (status == RequestStatus.OUVERTE) {
                    offer.setStatus(OfferStatus.EN_ATTENTE);
                } else if (h == 0) {
                    offer.setStatus(OfferStatus.ACCEPTEE);
                    accepted = helpers.get(h);
                } else {
                    offer.setStatus(OfferStatus.REFUSEE);
                }
                helpOfferRepository.save(offer);
            }
            if (status == RequestStatus.TERMINEE && accepted != null) {
                ratingRepository.save(rating(request, author, accepted, random));
                if (random.nextBoolean()) {
                    ratingRepository.save(rating(request, accepted, author, random));
                }
            }
        }
    }

    private Rating rating(HelpRequest request, User rater, User rated, Random random) {
        Rating rating = new Rating();
        rating.setRequest(request);
        rating.setRater(rater);
        rating.setRated(rated);
        rating.setScore(3 + random.nextInt(3));
        rating.setCreatedAt(request.getCompletedAt().plus(2, ChronoUnit.HOURS));
        return rating;
    }

    private List<User> pickHelpers(List<User> students, User author, int count, Random random) {
        List<User> helpers = new ArrayList<>();
        while (helpers.size() < count) {
            User candidate = students.get(random.nextInt(students.size()));
            if (!candidate.getId().equals(author.getId()) && !helpers.contains(candidate)) {
                helpers.add(candidate);
            }
        }
        return helpers;
    }
}
