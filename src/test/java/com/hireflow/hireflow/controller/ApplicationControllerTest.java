package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.JobListingSkill;
import com.hireflow.hireflow.data.model.ResumeProfile;
import com.hireflow.hireflow.data.model.ResumeProfileSkill;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.AiScreeningResultRepository;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.data.repository.CompanyRepository;
import com.hireflow.hireflow.data.repository.JobListingRepository;
import com.hireflow.hireflow.data.repository.ResumeProfileRepository;
import com.hireflow.hireflow.data.repository.SkillRepository;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.JobType;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.event.ApplicationSubmittedEvent;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.ai.AiScreeningEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private JobListingRepository jobListingRepository;
    @Autowired private ResumeProfileRepository resumeProfileRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private AiScreeningResultRepository aiScreeningResultRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private AiScreeningEventPublisher aiScreeningEventPublisher;

    private Company company;
    private Company otherCompany;
    private User applicant;
    private User otherApplicant;
    private User manager;
    private User otherManager;
    private Skill java;
    private Skill kafka;

    @BeforeEach
    void setUp() {
        deleteAllData();

        company = saveCompany("Acme");
        otherCompany = saveCompany("Other Co");

        applicant = saveUser("ada@example.com", Role.APPLICANT, null);
        otherApplicant = saveUser("grace@example.com", Role.APPLICANT, null);
        manager = saveUser("maya@example.com", Role.HMANAGER, company);
        otherManager = saveUser("other-manager@example.com", Role.HMANAGER, otherCompany);

        java = saveSkill("Java");
        kafka = saveSkill("Kafka");
    }

    @AfterEach
    void cleanUp() {
        reset(aiScreeningEventPublisher);
        deleteAllData();
    }

    private void deleteAllData() {
        aiScreeningResultRepository.deleteAll();
        applicationRepository.deleteAll();
        resumeProfileRepository.deleteAll();
        jobListingRepository.deleteAll();
        skillRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    @DisplayName("Should let an applicant apply to an open job and publish AI screening event")
    void applyToJob_success() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java, kafka));
        saveResumeProfile(applicant, List.of(java));

        mockMvc.perform(post("/api/v1/applications/jobs/" + job.getId())
                        .with(user(principalFor(applicant))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stage").value("SCREENING"))
                .andExpect(jsonPath("$.data.applicantId").value(applicant.getId()))
                .andExpect(jsonPath("$.data.jobListingId").value(job.getId()))
                .andExpect(jsonPath("$.data.companyId").value(company.getId()))
                .andExpect(jsonPath("$.data.stageUpdates.length()").value(2));

        List<Application> applications = applicationRepository.findAll();
        assertThat(applications).hasSize(1);
        Application persisted = applications.getFirst();
        assertThat(persisted.getStage()).isEqualTo(ApplicationStage.SCREENING);

        ArgumentCaptor<ApplicationSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationSubmittedEvent.class);
        verify(aiScreeningEventPublisher, timeout(1000)).publishApplicationSubmitted(eventCaptor.capture());
        ApplicationSubmittedEvent event = eventCaptor.getValue();
        assertThat(event.getApplicationId()).isEqualTo(persisted.getId());
        assertThat(event.getJobSkills()).containsExactlyInAnyOrder("Java", "Kafka");
        assertThat(event.getApplicantSkills()).containsExactly("Java");
        assertThat(event.getAutoRejectThreshold()).isEqualTo(40);
        assertThat(event.getAutoPassThreshold()).isEqualTo(75);
    }

    @Test
    @DisplayName("Should return 409 when applicant already applied to the job")
    void applyToJob_duplicate() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java));
        ResumeProfile profile = saveResumeProfile(applicant, List.of(java));
        saveApplication(applicant, job, profile, ApplicationStage.SCREENING);

        mockMvc.perform(post("/api/v1/applications/jobs/" + job.getId())
                        .with(user(principalFor(applicant))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already applied")));

        assertThat(applicationRepository.findAll()).hasSize(1);
        verify(aiScreeningEventPublisher, never()).publishApplicationSubmitted(any());
    }

    @Test
    @DisplayName("Should return 400 when applicant applies to a job that is not open")
    void applyToJob_closedJob() throws Exception {
        JobListing job = saveJob(company, JobStatus.CLOSED, List.of(java));
        saveResumeProfile(applicant, List.of(java));

        mockMvc.perform(post("/api/v1/applications/jobs/" + job.getId())
                        .with(user(principalFor(applicant))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not accepting applications")));

        assertThat(applicationRepository.findAll()).isEmpty();
        verify(aiScreeningEventPublisher, never()).publishApplicationSubmitted(any());
    }

    @Test
    @DisplayName("Should return 403 when hiring manager tries to apply")
    void applyToJob_forbiddenForManager() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java));

        mockMvc.perform(post("/api/v1/applications/jobs/" + job.getId())
                        .with(user(principalFor(manager))))
                .andExpect(status().isForbidden());

        assertThat(applicationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 404 when applicant has no resume profile")
    void applyToJob_missingResumeProfile() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java));

        mockMvc.perform(post("/api/v1/applications/jobs/" + job.getId())
                        .with(user(principalFor(applicant))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Resume profile not found")));

        assertThat(applicationRepository.findAll()).isEmpty();
        verify(aiScreeningEventPublisher, never()).publishApplicationSubmitted(any());
    }

    @Test
    @DisplayName("Should return only the signed-in applicant's applications")
    void findMyApplications_success() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java));
        ResumeProfile applicantProfile = saveResumeProfile(applicant, List.of(java));
        ResumeProfile otherProfile = saveResumeProfile(otherApplicant, List.of(kafka));
        Application ownApplication = saveApplication(applicant, job, applicantProfile, ApplicationStage.SCREENING);
        saveApplication(otherApplicant, job, otherProfile, ApplicationStage.REJECTED);

        mockMvc.perform(get("/api/v1/applications")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(principalFor(applicant))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(ownApplication.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Should block managers from the applicant applications collection")
    void findMyApplications_forbiddenForManager() throws Exception {
        mockMvc.perform(get("/api/v1/applications")
                        .with(user(principalFor(manager))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should let an applicant fetch their own application by id")
    void findMyApplication_success() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java));
        ResumeProfile profile = saveResumeProfile(applicant, List.of(java));
        Application application = saveApplication(applicant, job, profile, ApplicationStage.SCREENING);

        mockMvc.perform(get("/api/v1/applications/" + application.getId())
                        .with(user(principalFor(applicant))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(application.getId()))
                .andExpect(jsonPath("$.data.jobTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.data.applicantEmail").value("ada@example.com"));
    }

    @Test
    @DisplayName("Should return 404 when applicant fetches another applicant's application")
    void findMyApplication_notOwner() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java));
        ResumeProfile otherProfile = saveResumeProfile(otherApplicant, List.of(kafka));
        Application otherApplication = saveApplication(otherApplicant, job, otherProfile, ApplicationStage.SCREENING);

        mockMvc.perform(get("/api/v1/applications/" + otherApplication.getId())
                        .with(user(principalFor(applicant))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should let an owning company manager list applications for a job")
    void findByJob_success() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java));
        ResumeProfile applicantProfile = saveResumeProfile(applicant, List.of(java));
        ResumeProfile otherProfile = saveResumeProfile(otherApplicant, List.of(kafka));
        saveApplication(applicant, job, applicantProfile, ApplicationStage.SCREENING);
        saveApplication(otherApplicant, job, otherProfile, ApplicationStage.REJECTED);

        mockMvc.perform(get("/api/v1/applications/jobs/" + job.getId())
                        .param("page", "0")
                        .param("size", "5")
                        .with(user(principalFor(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].companyId").value(company.getId()));
    }

    @Test
    @DisplayName("Should return 403 when manager lists applications for another company's job")
    void findByJob_wrongCompany() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java));

        mockMvc.perform(get("/api/v1/applications/jobs/" + job.getId())
                        .with(user(principalFor(otherManager))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("your company")));
    }

    @Test
    @DisplayName("Should return 403 when applicant lists applications for a job")
    void findByJob_forbiddenForApplicant() throws Exception {
        JobListing job = saveJob(company, JobStatus.OPEN, List.of(java));

        mockMvc.perform(get("/api/v1/applications/jobs/" + job.getId())
                        .with(user(principalFor(applicant))))
                .andExpect(status().isForbidden());
    }

    private UserPrincipal principalFor(User user) {
        return new UserPrincipal(user);
    }

    private Company saveCompany(String name) {
        Company company = new Company();
        company.setName(name);
        return companyRepository.save(company);
    }

    private User saveUser(String email, Role role, Company company) {
        User user = new User("Test", "User", email, passwordEncoder.encode("password123"), role, true);
        user.setCompany(company);
        return userRepository.save(user);
    }

    private Skill saveSkill(String name) {
        return skillRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> createSkill(name));
    }

    private Skill createSkill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        return skillRepository.save(skill);
    }

    private JobListing saveJob(Company company, JobStatus status, List<Skill> skills) {
        JobListing job = new JobListing();
        job.setTitle("Backend Engineer");
        job.setType(JobType.FULL_TIME);
        job.setLocation("Remote");
        job.setSummary("Build APIs");
        job.setResponsibilities("Own backend services");
        job.setRequiredQualifications("Java and Kafka");
        job.setPreferredQualifications("Cloud experience");
        job.setStatus(status);
        job.setAutoRejectThreshold(40);
        job.setAutoPassThreshold(75);
        job.setCompany(company);
        for (Skill skill : skills) {
            job.getSkills().add(new JobListingSkill(job, skill));
        }
        return jobListingRepository.save(job);
    }

    private ResumeProfile saveResumeProfile(User user, List<Skill> skills) {
        ResumeProfile profile = new ResumeProfile();
        profile.setUser(user);
        profile.setSummary("Backend engineer with production Java experience");
        profile.setResumePdfUrl("https://cdn.example.com/resumes/" + user.getId() + ".pdf");
        for (Skill skill : skills) {
            profile.getSkills().add(new ResumeProfileSkill(profile, skill));
        }
        return resumeProfileRepository.save(profile);
    }

    private Application saveApplication(
            User applicant,
            JobListing job,
            ResumeProfile profile,
            ApplicationStage stage
    ) {
        Application application = new Application();
        application.setApplicant(applicant);
        application.setJobListing(job);
        application.setResumeProfile(profile);
        application.setCompanyId(job.getCompany().getId());
        application.setStage(stage);
        return applicationRepository.save(application);
    }
}
