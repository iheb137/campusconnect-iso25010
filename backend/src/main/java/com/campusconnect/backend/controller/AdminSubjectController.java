package com.campusconnect.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.SubjectInput;
import com.campusconnect.backend.dto.SubjectResponse;
import com.campusconnect.backend.service.SubjectService;

import jakarta.validation.Valid;

/** Création, modification et suppression des matières (rôle ADMIN). La lecture reste sur /api/subjects. */
@RestController
@RequestMapping("/api/admin/subjects")
public class AdminSubjectController {

    private final SubjectService subjectService;

    public AdminSubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SubjectInput input) {
        return subjectService.create(input, Long.valueOf(jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public SubjectResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @Valid @RequestBody SubjectInput input) {
        return subjectService.update(id, input, Long.valueOf(jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        subjectService.delete(id, Long.valueOf(jwt.getSubject()));
    }
}
