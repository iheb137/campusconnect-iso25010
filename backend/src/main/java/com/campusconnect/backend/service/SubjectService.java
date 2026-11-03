package com.campusconnect.backend.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.SubjectInput;
import com.campusconnect.backend.dto.SubjectResponse;
import com.campusconnect.backend.entity.AuditAction;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Matières : lecture pour tous, création / modification / suppression par l'administrateur.
 * RG-09 : le nom est unique (insensible à la casse) et une matière utilisée ne peut pas être supprimée.
 */
@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public SubjectService(
            SubjectRepository subjectRepository,
            HelpRequestRepository helpRequestRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.subjectRepository = subjectRepository;
        this.helpRequestRepository = helpRequestRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> findAll() {
        return subjectRepository.findAll(Sort.by("name")).stream()
                .map(SubjectResponse::from)
                .toList();
    }

    @Transactional
    public SubjectResponse create(SubjectInput input, Long adminId) {
        String name = input.name().trim();
        requireUniqueName(name, null);
        Subject subject = new Subject();
        subject.setName(name);
        Subject saved = save(subject);
        auditService.record(adminId, AuditAction.SUBJECT_CREATED, AuditService.SUBJECT, saved.getId(), name);
        return SubjectResponse.from(saved);
    }

    @Transactional
    public SubjectResponse update(Long subjectId, SubjectInput input, Long adminId) {
        Subject subject = find(subjectId);
        String name = input.name().trim();
        requireUniqueName(name, subjectId);
        String previous = subject.getName();
        subject.setName(name);
        save(subject);
        auditService.record(adminId, AuditAction.SUBJECT_UPDATED, AuditService.SUBJECT, subjectId,
                previous + " -> " + name);
        return SubjectResponse.from(subject);
    }

    @Transactional
    public void delete(Long subjectId, Long adminId) {
        Subject subject = find(subjectId);
        if (helpRequestRepository.existsBySubjectId(subjectId) || userRepository.existsBySkills_Id(subjectId)) {
            throw new ConflictException("Cette matière est utilisée par des demandes ou des profils : suppression impossible");
        }
        subjectRepository.delete(subject);
        auditService.record(adminId, AuditAction.SUBJECT_DELETED, AuditService.SUBJECT, subjectId, subject.getName());
    }

    private void requireUniqueName(String name, Long currentId) {
        subjectRepository.findAll().stream()
                .filter(other -> other.getName().equalsIgnoreCase(name))
                .filter(other -> !other.getId().equals(currentId))
                .findAny()
                .ifPresent(other -> {
                    throw new ConflictException("Une matière porte déjà ce nom");
                });
    }

    private Subject save(Subject subject) {
        try {
            return subjectRepository.saveAndFlush(subject);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Une matière porte déjà ce nom");
        }
    }

    private Subject find(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable"));
    }
}
