package com.campusconnect.backend.service;

import org.springframework.test.util.ReflectionTestUtils;

import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.entity.User;

/** Objets métier en mémoire pour les tests unitaires (aucune base de données). */
final class Fixtures {

    private Fixtures() {
    }

    static User user(long id, Role role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail("user" + id + "@test.tn");
        user.setFullName("Utilisateur " + id);
        user.setPasswordHash("hash");
        user.setRole(role);
        return user;
    }

    static User student(long id) {
        return user(id, Role.STUDENT);
    }

    static Subject subject(long id, String name) {
        Subject subject = new Subject();
        ReflectionTestUtils.setField(subject, "id", id);
        subject.setName(name);
        return subject;
    }

    static HelpRequest request(long id, User author, RequestStatus status) {
        HelpRequest request = new HelpRequest();
        ReflectionTestUtils.setField(request, "id", id);
        request.setTitle("Demande " + id);
        request.setDescription("Description de la demande " + id);
        request.setAvailability("Mardi soir");
        request.setLevel(Level.L2);
        request.setAuthor(author);
        request.setSubject(subject(1, "Algorithmique"));
        request.setStatus(status);
        return request;
    }

    static HelpOffer offer(long id, User helper, HelpRequest request, OfferStatus status) {
        HelpOffer offer = new HelpOffer();
        ReflectionTestUtils.setField(offer, "id", id);
        offer.setHelper(helper);
        offer.setRequest(request);
        offer.setMessage("Je peux aider");
        offer.setStatus(status);
        return offer;
    }
}
