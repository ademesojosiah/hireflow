package com.hireflow.hireflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.JobListingSkill;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.CompanyRepository;
import com.hireflow.hireflow.data.repository.JobListingRepository;
import com.hireflow.hireflow.data.repository.SkillRepository;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.dto.request.JobListingRequest;
import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.JobType;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobListingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobListingRepository jobListingRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockitoBean private JavaMailSender mailSender;

    private Company company;
    private User hManager;
    private User applicant;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setName("Acme");
        company = companyRepository.save(company);

        hManager = new User("Hannah", "Manager", "hannah@example.com", passwordEncoder.encode("password123"), Role.HMANAGER, true);
        hManager.setCompany(company);
        hManager = userRepository.save(hManager);

        applicant = userRepository.save(new User(
                "Bob", "Applicant", "bob@example.com", passwordEncoder.encode("password123"), Role.APPLICANT, true));
    }

    @AfterEach
    void cleanUp() {
        jobListingRepository.deleteAll();
        skillRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();
    }

    private UserPrincipal principalFor(User user) {
        return new UserPrincipal(user);
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

    private Skill saveSkill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        return skillRepository.save(skill);
    }

    @Test
    @DisplayName("Should create a job listing and persist it linked to the manager's company")
    void create_success() throws Exception {
        JobListingRequest request = sampleRequest(JobStatus.OPEN, 30, 80, null);

        mockMvc.perform(post("/api/v1/jobs")
                        .with(authentication(authenticated(principalFor(hManager), null, principalFor(hManager).getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Java Dev"))
                .andExpect(jsonPath("$.data.type").value("FULL_TIME"))
                .andExpect(jsonPath("$.data.companyId").value(company.getId()));

        assertThat(jobListingRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Should attach skills via the join table when skillIds are supplied")
    void create_withSkills() throws Exception {
        Skill java = saveSkill("Scala");
        Skill spring = saveSkill("Spring");
        JobListingRequest request = sampleRequest(JobStatus.OPEN, 30, 80, Set.of(java.getId(), spring.getId()));

        mockMvc.perform(post("/api/v1/jobs")
                        .with(authentication(authenticated(principalFor(hManager), null, principalFor(hManager).getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skills.length()").value(2));
        
    }

    @Test
    @DisplayName("Should return 404 when a supplied skillId does not exist")
    void create_unknownSkillId() throws Exception {
        Skill java = saveSkill("Scala");
        JobListingRequest request = sampleRequest(JobStatus.OPEN, 30, 80, Set.of(java.getId(), "skill-does-not-exist"));

        mockMvc.perform(post("/api/v1/jobs")
                        .with(authentication(authenticated(principalFor(hManager), null, principalFor(hManager).getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        assertThat(jobListingRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 403 when applicant attempts to create a job listing")
    void create_forbiddenForApplicant() throws Exception {
        JobListingRequest request = sampleRequest(JobStatus.OPEN, 30, 80, null);

        mockMvc.perform(post("/api/v1/jobs")
                        .with(authentication(authenticated(principalFor(applicant), null, principalFor(applicant).getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(jobListingRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 400 when thresholds are invalid")
    void create_invalidThresholds() throws Exception {
        JobListingRequest request = sampleRequest(JobStatus.OPEN, 80, 80, null);

        mockMvc.perform(post("/api/v1/jobs")
                        .with(authentication(authenticated(principalFor(hManager), null, principalFor(hManager).getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should allow the owning company manager to update the job listing")
    void update_success() throws Exception {
        JobListing job = new JobListing();
        job.setTitle("Old Title");
        job.setType(JobType.PART_TIME);
        job.setLocation("Lagos");
        job.setSummary("Old summary");
        job.setResponsibilities("Old responsibilities text.");
        job.setRequiredQualifications("Old required qualifications text.");
        job.setStatus(JobStatus.DRAFT);
        job.setAutoRejectThreshold(20);
        job.setAutoPassThreshold(70);
        job.setCompany(company);
        job = jobListingRepository.save(job);

        JobListingRequest request = sampleRequest(JobStatus.OPEN, 30, 80, null);

        mockMvc.perform(put("/api/v1/jobs/" + job.getId())
                        .with(authentication(authenticated(principalFor(hManager), null, principalFor(hManager).getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java Dev"))
                .andExpect(jsonPath("$.data.type").value("FULL_TIME"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @DisplayName("Should return a paginated page of jobs filtered by company")
    void findByCompany_paginated() throws Exception {
        JobListing first = new JobListing();
        first.setTitle("Backend Engineer");
        first.setType(JobType.FULL_TIME);
        first.setSummary("Backend role summary");
        first.setResponsibilities("Build backend services");
        first.setRequiredQualifications("3+ years Java");
        first.setStatus(JobStatus.OPEN);
        first.setAutoRejectThreshold(20);
        first.setAutoPassThreshold(70);
        first.setCompany(company);
        jobListingRepository.save(first);

        JobListing second = new JobListing();
        second.setTitle("Frontend Engineer");
        second.setType(JobType.CONTRACT);
        second.setSummary("Frontend role summary");
        second.setResponsibilities("Build UI");
        second.setRequiredQualifications("React experience");
        second.setStatus(JobStatus.OPEN);
        second.setAutoRejectThreshold(20);
        second.setAutoPassThreshold(70);
        second.setCompany(company);
        jobListingRepository.save(second);

        mockMvc.perform(get("/api/v1/jobs/company/" + company.getId())
                        .param("page", "0")
                        .param("size", "1")
                        .with(authentication(authenticated(principalFor(applicant), null, principalFor(applicant).getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.size").value(1));
    }

    @Test
    @DisplayName("Should use default pagination params when none provided")
    void findByCompany_defaultPagination() throws Exception {
        JobListing job = new JobListing();
        job.setTitle("Default Pagination Job");
        job.setType(JobType.FULL_TIME);
        job.setSummary("Summary");
        job.setResponsibilities("Responsibilities");
        job.setRequiredQualifications("Qualifications");
        job.setStatus(JobStatus.OPEN);
        job.setAutoRejectThreshold(20);
        job.setAutoPassThreshold(70);
        job.setCompany(company);
        jobListingRepository.save(job);

        mockMvc.perform(get("/api/v1/jobs/company/" + company.getId())
                        .with(authentication(authenticated(principalFor(applicant), null, principalFor(applicant).getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.content[0].title").value("Default Pagination Job"));
    }

    @Test
    @DisplayName("Should return 404 when fetching a non-existent job listing")
    void findById_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/missing-id")
                        .with(authentication(authenticated(principalFor(applicant), null, principalFor(applicant).getAuthorities()))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should allow the owning manager to delete a job listing")
    void delete_success() throws Exception {
        JobListing job = new JobListing();
        job.setTitle("To Delete");
        job.setType(JobType.CONTRACT);
        job.setSummary("Short summary");
        job.setResponsibilities("Some responsibilities.");
        job.setRequiredQualifications("Some required qualifications.");
        job.setStatus(JobStatus.DRAFT);
        job.setAutoRejectThreshold(20);
        job.setAutoPassThreshold(70);
        job.setCompany(company);
        job = jobListingRepository.save(job);

        mockMvc.perform(delete("/api/v1/jobs/" + job.getId())
                        .with(authentication(authenticated(principalFor(hManager), null, principalFor(hManager).getAuthorities()))))
                .andExpect(status().isOk());

        assertThat(jobListingRepository.findById(job.getId())).isEmpty();
    }
}
