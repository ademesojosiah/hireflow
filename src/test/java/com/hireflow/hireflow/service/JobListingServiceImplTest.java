package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.JobListingRepository;
import com.hireflow.hireflow.dto.request.JobListingRequest;
import com.hireflow.hireflow.dto.response.JobListingFilterResponse;
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
    @Mock private SkillService skillService;
    @Mock private CompanyService companyService;
    @Mock private UserService userService;
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

        lenient().when(companyService.findCompanyById("company-1")).thenReturn(company);
        lenient().when(companyService.findCompanyById("company-2")).thenReturn(otherCompany);
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

    private JobListingFilterResponse sampleJobListingFilterResponse() {
        return new JobListingFilterResponse(
                "job-1", "Java Dev", JobType.FULL_TIME, "Remote",
                "Join us to build APIs", "Design, build, and ship high-quality REST APIs.",
                "5+ years Java, Spring Boot, MySQL.", "Experience with Kafka and AWS.",
                JobStatus.OPEN, "company-1", "Acme", List.of());
    }

    private JobListingRequest fullStackEngineerRequest(Set<String> skillIds) {
        return new JobListingRequest(
                "Full stack engineer",
                JobType.FULL_TIME,
                "r",
                "<p>We are looking for a Backend Developer to build scalable APIs and maintain cloud infrastructure.</p>",
                "<p>Responsibilities:</p><ul><li><p>Develop REST APIs</p></li><li><p>Maintain database performance</p></li><li><p>Collaborate with frontend engineers</p></li></ul><p></p>",
                "<p>Required Qualifications:</p><ul><li><p>3+ years of Java experience</p></li><li><p>Experience with Spring Boot</p></li><li><p>Knowledge of SQL databases</p></li></ul><p></p>",
                "<p>Preferred Qualifications:</p><ul><li><p>AWS experience</p></li><li><p>Docker/Kubernetes knowledge</p></li><li><p>Previous startup experience</p></li></ul><p></p>",
                JobStatus.OPEN,
                40,
                75,
                skillIds
        );
    }

    @Test
    @DisplayName("Should create a job listing for the manager's own company")
    void create_success() {
        when(userService.findUserById(hManager.getId())).thenReturn(hManager);
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

        when(userService.findUserById(hManager.getId())).thenReturn(hManager);
        when(skillService.findAllByIds(ids)).thenReturn(resolved);
        when(jobListingMapper.toEntity(eq(withSkills), eq(company), eq(resolved))).thenReturn(job);
        when(jobListingRepository.save(job)).thenReturn(job);
        when(jobListingMapper.toResponse(job)).thenReturn(sampleResponse());

        jobListingService.create(withSkills, hManager);

        verify(skillService).findAllByIds(ids);
        verify(jobListingMapper).toEntity(eq(withSkills), eq(company), eq(resolved));
    }

    @Test
    @DisplayName("Should create full stack engineer job using company service and skill service")
    void create_fullStackEngineerPayload() {
        Set<String> ids = Set.of("skill-1", "skill-2", "skill-3");
        JobListingRequest payload = fullStackEngineerRequest(ids);
        Skill backend = new Skill();
        backend.setId("skill-1");
        backend.setName("Backend Engineering");
        Skill cloud = new Skill();
        cloud.setId("skill-2");
        cloud.setName("Cloud Infrastructure");
        Skill database = new Skill();
        database.setId("skill-3");
        database.setName("Database Performance");
        List<Skill> resolved = List.of(backend, cloud, database);

        JobListing fullStackJob = new JobListing();
        fullStackJob.setId("job-full-stack");
        fullStackJob.setTitle(payload.getTitle());
        fullStackJob.setType(payload.getType());
        fullStackJob.setLocation(payload.getLocation());
        fullStackJob.setSummary(payload.getSummary());
        fullStackJob.setResponsibilities(payload.getResponsibilities());
        fullStackJob.setRequiredQualifications(payload.getRequiredQualifications());
        fullStackJob.setPreferredQualifications(payload.getPreferredQualifications());
        fullStackJob.setStatus(payload.getStatus());
        fullStackJob.setAutoRejectThreshold(payload.getAutoRejectThreshold());
        fullStackJob.setAutoPassThreshold(payload.getAutoPassThreshold());
        fullStackJob.setCompany(company);

        JobListingResponse response = new JobListingResponse(
                "job-full-stack",
                payload.getTitle(),
                payload.getType(),
                payload.getLocation(),
                payload.getSummary(),
                payload.getResponsibilities(),
                payload.getRequiredQualifications(),
                payload.getPreferredQualifications(),
                payload.getStatus(),
                payload.getAutoRejectThreshold(),
                payload.getAutoPassThreshold(),
                "company-1",
                "Acme",
                List.of()
        );

        when(userService.findUserById(hManager.getId())).thenReturn(hManager);
        when(skillService.findAllByIds(ids)).thenReturn(resolved);
        when(jobListingMapper.toEntity(payload, company, resolved)).thenReturn(fullStackJob);
        when(jobListingRepository.save(fullStackJob)).thenReturn(fullStackJob);
        when(jobListingMapper.toResponse(fullStackJob)).thenReturn(response);

        JobListingResponse result = jobListingService.create(payload, hManager);

        assertThat(result.getTitle()).isEqualTo("Full stack engineer");
        assertThat(result.getSummary()).isEqualTo(payload.getSummary());
        assertThat(result.getAutoRejectThreshold()).isEqualTo(40);
        assertThat(result.getAutoPassThreshold()).isEqualTo(75);
        verify(userService).findUserById("user-1");
        verify(companyService).findCompanyById("company-1");
        verify(skillService).findAllByIds(ids);
        verify(jobListingMapper).toEntity(payload, company, resolved);
        verify(jobListingRepository).save(fullStackJob);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when one or more skill IDs are unknown")
    void create_unknownSkillId() {
        Set<String> ids = Set.of("skill-1", "skill-missing");
        JobListingRequest withSkills = sampleRequest(JobStatus.OPEN, 30, 80, ids);
        when(userService.findUserById(hManager.getId())).thenReturn(hManager);
        when(skillService.findAllByIds(ids))
                .thenThrow(new ResourceNotFoundException("One or more skills not found"));

        assertThatThrownBy(() -> jobListingService.create(withSkills, hManager))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("skills not found");
        verify(jobListingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when applicant tries to create a job listing")
    void create_forbiddenForApplicant() {
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);

        assertThatThrownBy(() -> jobListingService.create(request, applicant))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only admins and hiring managers");
        verify(jobListingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when creating without authentication")
    void create_nullUser() {
        assertThatThrownBy(() -> jobListingService.create(request, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");

        verifyNoInteractions(userService);
        verify(jobListingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when manager has no company")
    void create_managerWithoutCompany() {
        hManager.setCompany(null);
        when(userService.findUserById(hManager.getId())).thenReturn(hManager);

        assertThatThrownBy(() -> jobListingService.create(request, hManager))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("belong to a company");
    }

    @Test
    @DisplayName("Should reject when auto-reject threshold is not less than auto-pass threshold")
    void create_invalidThresholds() {
        JobListingRequest invalid = sampleRequest(JobStatus.OPEN, 80, 80, null);
        when(userService.findUserById(hManager.getId())).thenReturn(hManager);
        assertThatThrownBy(() -> jobListingService.create(invalid, hManager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Auto-reject threshold");
    }

    @Test
    @DisplayName("Should update a job listing owned by the manager's company")
    void update_success() {
        when(userService.findUserById(hManager.getId())).thenReturn(hManager);
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

        when(userService.findUserById(hManager.getId())).thenReturn(hManager);
        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(skillService.findAllByIds(ids)).thenReturn(resolved);
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
        when(userService.findUserById(hManager.getId())).thenReturn(hManager);

        assertThatThrownBy(() -> jobListingService.update("job-1", request, hManager))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("your own company");
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
        when(userService.findUserById(hManager.getId())).thenReturn(hManager);
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
    @DisplayName("Should return current manager's company job listings")
    void findByCompanyForCurrentManager_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobListing> page = new PageImpl<>(List.of(job), pageable, 1);
        when(userService.findUserById(hManager.getId())).thenReturn(hManager);
        when(jobListingRepository.findAllByCompany_Id("company-1", pageable)).thenReturn(page);
        when(jobListingMapper.toResponse(job)).thenReturn(sampleResponse());

        Page<JobListingResponse> result = jobListingService.findByCompany(null, hManager, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(companyService).findCompanyById("company-1");
        verify(jobListingRepository).findAllByCompany_Id("company-1", pageable);
    }

    @Test
    @DisplayName("Should normalize blank open-job title filter to null")
    void findAllOpen_blankTitleFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(jobListingRepository.findAllOpen(isNull(), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        jobListingService.findAllOpen("   ", null, pageable);

        verify(jobListingRepository).findAllOpen(isNull(), isNull(), eq(pageable));
    }

    @Test
    @DisplayName("Should return open job listings filtered by title and job type")
    void findAllOpen_filteredByTitleAndType() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobListing> page = new PageImpl<>(List.of(job), pageable, 1);
        when(jobListingRepository.findAllOpen("Engineer", JobType.FULL_TIME, pageable)).thenReturn(page);
        when(jobListingMapper.toJobListingFilterResponse(job)).thenReturn(sampleJobListingFilterResponse());

        Page<JobListingFilterResponse> result = jobListingService.findAllOpen("  Engineer  ", JobType.FULL_TIME, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(jobListingRepository).findAllOpen("Engineer", JobType.FULL_TIME, pageable);
        verify(jobListingRepository, never()).findAllByStatus(any(), any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when applicant tries to delete a job")
    void delete_forbiddenForApplicant() {
        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);

        assertThatThrownBy(() -> jobListingService.delete("job-1", applicant))
                .isInstanceOf(AccessDeniedException.class);
        verify(jobListingRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when manager from another company tries to delete")
    void delete_forbiddenForOtherCompany() {
        hManager.setCompany(otherCompany);
        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(userService.findUserById(hManager.getId())).thenReturn(hManager);

        assertThatThrownBy(() -> jobListingService.delete("job-1", hManager))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("your own company");

        verify(jobListingRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting an unknown job")
    void delete_notFound() {
        when(jobListingRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobListingService.delete("missing", hManager))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Job listing not found");

        verify(jobListingRepository, never()).delete(any());
    }
}
