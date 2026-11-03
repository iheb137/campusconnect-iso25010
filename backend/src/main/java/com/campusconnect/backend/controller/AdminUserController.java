package com.campusconnect.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.AdminUserResponse;
import com.campusconnect.backend.dto.EnabledUpdateRequest;
import com.campusconnect.backend.dto.PageResponse;
import com.campusconnect.backend.dto.RoleUpdateRequest;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.service.AdminUserService;

import jakarta.validation.Valid;

/** Gestion des comptes (rôle ADMIN). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public PageResponse<AdminUserResponse> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "role", required = false) Role role,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return adminUserService.search(query, role, page, size);
    }

    @PutMapping("/{id}/role")
    public AdminUserResponse changeRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return adminUserService.changeRole(id, request.role(), Long.valueOf(jwt.getSubject()));
    }

    @PutMapping("/{id}/enabled")
    public AdminUserResponse changeEnabled(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @Valid @RequestBody EnabledUpdateRequest request) {
        return adminUserService.changeEnabled(id, request.enabled(), Long.valueOf(jwt.getSubject()));
    }
}
