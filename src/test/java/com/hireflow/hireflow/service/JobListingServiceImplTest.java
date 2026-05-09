package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.JobListingRepository;
import com.hireflow.hireflow.data.repository.SkillRepository;
import com.hireflow.hireflow.dto.request.JobListingRequest;
import com.hireflow.hireflow.dto.response.JobListingResponse;
import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.JobType;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.JobListingMapper;
import com.hireflow.hireflow.service.impl.JobListingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobListingServiceImplTest {

    @Mock private JobListingRepository jobListingRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private JobListingMapper jobListingMapper;
    @InjectMocks private JobListingServiceImpl jobListingService;

    private Company company;
    private Company otherCompany;
    private User hManager;
    private User applicant;
    private JobListing job;
    private JobListingRequest request;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId("company-1");
        company.setName("Acme");

        otherCompany = new Company();
        otherCompany.setId("company-2");

        hManager = new User();
        hManager.setId("user-1");
        hManager.setRole(Role.HMANAGER);
        hManager.setCompany(company);

        applicant = new User();
        applicant.setId("user-2");
        applicant.setRole(Role.APPLICANT);

        job = new JobListing();
        job.setId("job-1");
        job.setTitle("Java Dev");
        job.setStatus(JobStatus.DRAFT);
        job.setCompany(company);

        request = sampleRequest(JobStatus.OPEN, 30, 80, null);
    }

    private JobListingRequest sampleRequest(JobStatus status, int autoReject, int autoPass, Set<String> skillIds) {
        return new JobListingRequest(
                "Java Dev",
                JobType.FULL_TIME,
                "Remote",
                "Join us to build APIs",
                "Design, build, and ship high-quality REST APIs.",
                "5+ years Java, Spring Boot, MySQL.",
                "Experience with Kafka and AWS.",
                status,
                autoReject,
                autoPass,
                skillIds
        );
    }

    private JobListingResponse sampleResponse() {
        return new JobListingResponse(
                "job-1", "Java Dev", JobType.FULL_TIME, "Remote",
                "Join us to build APIs", "Design, build, and ship high-quality REST APIs.",
                "5+ years Java, Spring Boot, MySQL.", "Experience with Kafka and AWS.",
                JobStatus.OPEN, 30, 80, "company-1", "Acme", List.of());
    }

    @Test
    @DisplayName("Should create a job listing for the manager's own company")
    void create_success() {
        when(jobListingMapper.toEntity(eq(request), eq(company), anyList())).thenReturn(job);
        when(jobListingRepository.save(job)).thenReturn(job);
        when(jobListingMapper.toResponse(job)).thenReturn(sampleResponse());

        JobListingResponse response = jobListingService.create(request, hManager);

        assertThat(response.getCompanyId()).isEqualTo("company-1");
        verify(jobListingRepository).save(job);
    }

    @Test
    @DisplayName("Should resolve and attach skills when skillIds are provided")
    void create_withSkills() {
        Skill java = new Skill();
        java.setId("skill-1");
        java.setName("Java");
        Skill spring = new Skill();
        spring.setId("skill-2");
        spring.setName("Spring");
        Set<String> ids = Set.of("skill-1", "skill-2");
        JobListingRequest withSkills = sampleRequest(JobStatus.OPEN, 30, 80, ids);
        List<Skill> resolved = List.of(java, spring);

        when(skillRepository.findAllById(ids)).thenReturn(resolved);
        when(jobListingMapper.toEntity(eq(withSkills), eq(company), eq(resolved))).thenReturn(job);
        when(jobListingRepository.save(job)).thenReturn(job);
        when(jobListingMapper.toResponse(job)).thenReturn(sampleResponse());

        jobListingService.create(withSkills, hManager);

        verify(skillRepository).findAllById(ids);
        verify(jobListingMapper).toEntity(eq(withSkills), eq(company), eq(resolved));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when one or more skill IDs are unknown")
    void create_unknownSkillId() {
        Set<String> ids = Set.of("skill-1", "skill-missing");
        JobListingRequest withSkills = sampleRequest(JobStatus.OPEN, 30, 80, ids);
        Skill onlyOne = new Skill();
        onlyOne.setId("skill-1");

        when(skillRepository.findAllById(ids)).thenReturn(List.of(onlyOne));

        assertThatThrownBy(() -> jobListingService.create(withSkills, hManager))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("skills not found");
        verify(jobListingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when applicant tries to create a job listing")
    void create_forbiddenForApplicant() {
        assertThatThrownBy(() -> jobListingService.create(request, applicant))
                .isInstanceOf(AccessDeniedException.class);
        verify(jobListingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when manager has no company")
    void create_managerWithoutCompany() {
        hManager.setCompany(null);
        assertThatThrownBy(() -> jobListingService.create(request, hManager))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should reject when auto-reject threshold is not less than auto-pass threshold")
    void create_invalidThresholds() {
        JobListingRequest invalid = sampleRequest(JobStatus.OPEN, 80, 80, null);
        assertThatThrownBy(() -> jobListingService.create(invalid, hManager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Auto-reject threshold");
    }

    @Test
    @DisplayName("Should update a job listing owned by the manager's company")
    void update_success() {
        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobListingRepository.save(job)).thenReturn(job);
        when(jobListingMapper.toResponse(job)).thenReturn(sampleResponse());

        jobListingService.update("job-1", request, hManager);

        verify(jobListingMapper).applyUpdate(eq(job), eq(request), isNull());
    }

    @Test
    @DisplayName("Should resolve skills on update when skillIds are supplied")
    void update_withSkills() {
        Skill java = new Skill();
        java.setId("skill-1");
        Set<String> ids = Set.of("skill-1");
        JobListingRequest withSkills = sampleRequest(JobStatus.OPEN, 30, 80, ids);
        List<Skill> resolved = List.of(java);

        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(skillRepository.findAllById(ids)).thenReturn(resolved);
        when(jobListingRepository.save(job)).thenReturn(job);
        when(jobListingMapper.toResponse(job)).thenReturn(sampleResponse());

        jobListingService.update("job-1", withSkills, hManager);

        verify(jobListingMapper).applyUpdate(eq(job), eq(withSkills), eq(resolved));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when manager from another company tries to update")
    void update_forbiddenForOtherCompany() {
        hManager.setCompany(otherCompany);
        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobListingService.update("job-1", request, hManager))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent job")
    void update_notFound() {
        when(jobListingRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobListingService.update("missing", request, hManager))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should delete a job listing owned by the manager's company")
    void delete_success() {
        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job));

        jobListingService.delete("job-1", hManager);

        verify(jobListingRepository).delete(job);
    }

    @Test
    @DisplayName("Should return a paginated page of job listings for a company")
    void findByCompany_paginated() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobListing> page = new PageImpl<>(List.of(job), pageable, 1);
        when(jobListingRepository.findAllByCompany_Id("company-1", pageable)).thenReturn(page);
        when(jobListingMapper.toResponse(job)).thenReturn(sampleResponse());

        Page<JobListingResponse> result = jobListingService.findByCompany("company-1", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(jobListingRepository).findAllByCompany_Id("company-1", pageable);
        verify(jobListingRepository, never()).findAllByCompany_IdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Should filter by status when status param is supplied")
    void findByCompany_filteredByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobListing> page = new PageImpl<>(List.of(job), pageable, 1);
        when(jobListingRepository.findAllByCompany_IdAndStatus("company-1", JobStatus.OPEN, pageable)).thenReturn(page);
        when(jobListingMapper.toResponse(job)).thenReturn(sampleResponse());

        Page<JobListingResponse> result = jobListingService.findByCompany("company-1", JobStatus.OPEN, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(jobListingRepository).findAllByCompany_IdAndStatus("company-1", JobStatus.OPEN, pageable);
        verify(jobListingRepository, never()).findAllByCompany_Id(any(), any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when applicant tries to delete a job")
    void delete_forbiddenForApplicant() {
        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobListingService.delete("job-1", applicant))
                .isInstanceOf(AccessDeniedException.class);
        verify(jobListingRepository, never()).delete(any());
    }
}
