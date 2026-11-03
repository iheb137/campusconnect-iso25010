package com.campusconnect.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.HelpRequestInput;
import com.campusconnect.backend.dto.HelpRequestResponse;
import com.campusconnect.backend.dto.PageResponse;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.service.HelpRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/requests")
public class HelpRequestController {

    private final HelpRequestService helpRequestService;

    public HelpRequestController(HelpRequestService helpRequestService) {
        this.helpRequestService = helpRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HelpRequestResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HelpRequestInput input) {
        return helpRequestService.create(currentUserId(jwt), input);
    }

    @GetMapping
    public PageResponse<HelpRequestResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "subjectId", required = false) Long subjectId,
            @RequestParam(name = "level", required = false) Level level,
            @RequestParam(name = "status", required = false) RequestStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return helpRequestService.search(subjectId, level, status, page, size, currentUserId(jwt));
    }

    @GetMapping("/{id}")
    public HelpRequestResponse getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id) {
        return helpRequestService.getById(id, currentUserId(jwt));
    }

    @PutMapping("/{id}")
    public HelpRequestResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @Valid @RequestBody HelpRequestInput input) {
        return helpRequestService.update(id, currentUserId(jwt), input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id) {
        helpRequestService.delete(id, currentUserId(jwt));
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}