package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.AiScreeningResult;
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
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.JobType;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.event.events.ScreeningCompletedEvent;
import com.hireflow.hireflow.event.producer.AiScreeningEventProducer;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.ApplicationMapper;
import com.hireflow.hireflow.service.impl.ApplicationSubmissionPersistence;
import com.hireflow.hireflow.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private AiScreeningResultRepository aiScreeningResultRepository;
    @Mock private UserService userService;
    @Mock private JobListingService jobListingService;
    @Mock private ResumeProfileService resumeProfileService;
    @Mock private AiScreeningEventProducer aiScreeningEventProducer;
    @Mock private ApplicationMapper applicationMapper;
    private ApplicationServiceImpl applicationService;

    private Company company;
    private Company otherCompany;
    private User applicant;
    private User manager;
    private JobListing job;
    private ResumeProfile resumeProfile;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId("company-1");
        company.setName("Acme");

        otherCompany = new Company();
        otherCompany.setId("company-2");
        otherCompany.setName("Other Co");

        applicant = new User("Ada", "Applicant", "ada@example.com", "password", Role.APPLICANT, true);
        applicant.setId("applicant-1");

        manager = new User("Maya", "Manager", "maya@example.com", "password", Role.HMANAGER, true);
        manager.setId("manager-1");
        manager.setCompany(company);

        job = sampleJob(company, JobStatus.OPEN);
        resumeProfile = sampleResumeProfile(applicant);

        ApplicationSubmissionPersistence applicationSubmissionPersistence = new ApplicationSubmissionPersistence(
                applicationRepository,
                userService,
                jobListingService,
                resumeProfileService,
                applicationMapper
        );
        applicationService = new ApplicationServiceImpl(
                applicationRepository,
                aiScreeningResultRepository,
                userService,
                jobListingService,
                aiScreeningEventProducer,
                applicationMapper,
                applicationSubmissionPersistence
        );
    }

    @Test
    @DisplayName("Should submit application, save screening stage, and publish AI screening event")
    void applyToJob_success() {
        ApplicationResponse expected = new ApplicationResponse();
        expected.setId("application-1");

        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(resumeProfileService.findProfileByUserId(applicant.getId())).thenReturn(resumeProfile);
        when(applicationRepository.existsByApplicant_IdAndJobListing_Id(applicant.getId(), job.getId())).thenReturn(false);
        AtomicReference<Application> savedApplication = new AtomicReference<>();
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application saved = invocation.getArgument(0);
            saved.setId("application-1");
            savedApplication.set(saved);
            return saved;
        });
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(expected);

        ApplicationResponse response = applicationService.applyToJob(job.getId(), applicant);

        assertThat(response).isSameAs(expected);

        assertThat(savedApplication.get().getApplicant()).isSameAs(applicant);
        assertThat(savedApplication.get().getJobListing()).isSameAs(job);
        assertThat(savedApplication.get().getResumeProfile()).isSameAs(resumeProfile);
        assertThat(savedApplication.get().getCompanyId()).isEqualTo(company.getId());
        assertThat(savedApplication.get().getStage()).isEqualTo(ApplicationStage.SCREENING);
        assertThat(savedApplication.get().getStageUpdates())
                .extracting("currentStage")
                .containsExactly(ApplicationStage.APPLIED, ApplicationStage.SCREENING);

        verify(aiScreeningEventProducer).publishApplicationSubmittedAsync(argThat(event ->
                event != null
                        && "application-1".equals(event.getApplicationId())
                        && "job-1".equals(event.getJobListingId())
                        && "Backend Engineer".equals(event.getJobTitle())
                        && "Build APIs".equals(event.getJobSummary())
                        && "Java and Kafka".equals(event.getRequiredQualifications())
                        && "Cloud experience".equals(event.getPreferredQualifications())
                        && "applicant-1".equals(event.getApplicantId())
                        && "ada@example.com".equals(event.getApplicantEmail())
                        && "Backend engineer with Java experience".equals(event.getResumeSummary())
                        && "https://cdn.example.com/resume.pdf".equals(event.getResumePdfUrl())
                        && event.getJobSkills().equals(List.of("Java", "Kafka"))
                        && event.getApplicantSkills().equals(List.of("Java"))
                        && Integer.valueOf(40).equals(event.getAutoRejectThreshold())
                        && Integer.valueOf(75).equals(event.getAutoPassThreshold())
        ));
    }

    @Test
    @DisplayName("Should reject application submission from a non-applicant")
    void applyToJob_forbiddenForNonApplicant() {
        manager.setRole(Role.HMANAGER);
        when(userService.findUserById(manager.getId())).thenReturn(manager);

        assertThatThrownBy(() -> applicationService.applyToJob(job.getId(), manager))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only applicants");

        verify(applicationRepository, never()).save(any());
        verify(aiScreeningEventProducer, never()).publishApplicationSubmittedAsync(any());
    }

    @Test
    @DisplayName("Should reject application submission without authentication")
    void applyToJob_nullUser() {
        assertThatThrownBy(() -> applicationService.applyToJob(job.getId(), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");

        verifyNoInteractions(jobListingService, resumeProfileService, aiScreeningEventProducer);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject application submission for a closed job")
    void applyToJob_closedJob() {
        job.setStatus(JobStatus.CLOSED);
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);

        assertThatThrownBy(() -> applicationService.applyToJob(job.getId(), applicant))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("not accepting applications");

        verify(resumeProfileService, never()).findProfileByUserId(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject duplicate applications for the same applicant and job")
    void applyToJob_duplicateApplication() {
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(resumeProfileService.findProfileByUserId(applicant.getId())).thenReturn(resumeProfile);
        when(applicationRepository.existsByApplicant_IdAndJobListing_Id(applicant.getId(), job.getId())).thenReturn(true);

        assertThatThrownBy(() -> applicationService.applyToJob(job.getId(), applicant))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
        verify(aiScreeningEventProducer, never()).publishApplicationSubmittedAsync(any());
    }

    @Test
    @DisplayName("Should return profile lookup failure without publishing an event")
    void applyToJob_missingResumeProfile() {
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(resumeProfileService.findProfileByUserId(applicant.getId()))
                .thenThrow(new ResourceNotFoundException("Resume profile not found"));

        assertThatThrownBy(() -> applicationService.applyToJob(job.getId(), applicant))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile");

        verify(applicationRepository, never()).save(any());
        verify(aiScreeningEventProducer, never()).publishApplicationSubmittedAsync(any());
    }

    @Test
    @DisplayName("Should wrap unexpected submission failures in a custom exception")
    void applyToJob_unexpectedFailure() {
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(jobListingService.findJobListingById(job.getId())).thenThrow(new IllegalStateException("broker down"));

        assertThatThrownBy(() -> applicationService.applyToJob(job.getId(), applicant))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Application submission failed");
    }

    @Test
    @DisplayName("Should return current applicant's applications as a page")
    void findMyApplications_success() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ApplicationResponse mapped = new ApplicationResponse();
        mapped.setId(application.getId());
        PageRequest pageable = PageRequest.of(0, 10);
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(applicationRepository.findAllByApplicant_Id(applicant.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(applicationMapper.toResponse(application)).thenReturn(mapped);

        Page<ApplicationResponse> response = applicationService.findMyApplications(applicant, pageable);

        assertThat(response.getContent()).containsExactly(mapped);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should require authentication before listing applicant applications")
    void findMyApplications_nullUser() {
        assertThatThrownBy(() -> applicationService.findMyApplications(null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("Should return one application when it belongs to the applicant")
    void findMyApplication_success() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ApplicationResponse mapped = new ApplicationResponse();
        mapped.setId(application.getId());

        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(applicationRepository.findByIdAndApplicant_Id(application.getId(), applicant.getId()))
                .thenReturn(Optional.of(application));
        when(applicationMapper.toResponse(application)).thenReturn(mapped);

        ApplicationResponse response = applicationService.findMyApplication(application.getId(), applicant);

        assertThat(response).isSameAs(mapped);
    }

    @Test
    @DisplayName("Should return not found when an applicant requests someone else's application")
    void findMyApplication_notFound() {
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(applicationRepository.findByIdAndApplicant_Id("missing", applicant.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.findMyApplication("missing", applicant))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    @DisplayName("Should let a company manager list applications for their own job")
    void findByJob_success() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ApplicationResponse mapped = new ApplicationResponse();
        mapped.setId(application.getId());
        PageRequest pageable = PageRequest.of(0, 5);

        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(applicationRepository.findAllByJobListing_IdAndCompanyId(job.getId(), company.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(applicationMapper.toResponse(application)).thenReturn(mapped);

        Page<ApplicationResponse> response = applicationService.findByJob(job.getId(), manager, pageable);

        assertThat(response.getContent()).containsExactly(mapped);
        verify(applicationRepository).findAllByJobListing_IdAndCompanyId(job.getId(), company.getId(), pageable);
    }

    @Test
    @DisplayName("Should let an admin list applications for their company job")
    void findByJob_adminSuccess() {
        manager.setRole(Role.ADMIN);
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ApplicationResponse mapped = new ApplicationResponse();
        mapped.setId(application.getId());
        PageRequest pageable = PageRequest.of(0, 5);

        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(applicationRepository.findAllByJobListing_IdAndCompanyId(job.getId(), company.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(applicationMapper.toResponse(application)).thenReturn(mapped);

        Page<ApplicationResponse> response = applicationService.findByJob(job.getId(), manager, pageable);

        assertThat(response.getContent()).containsExactly(mapped);
    }

    @Test
    @DisplayName("Should reject job application listing without authentication")
    void findByJob_nullUser() {
        assertThatThrownBy(() -> applicationService.findByJob(job.getId(), null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");

        verifyNoInteractions(jobListingService);
        verify(applicationRepository, never()).findAllByJobListing_IdAndCompanyId(any(), any(), any());
    }

    @Test
    @DisplayName("Should block managers from viewing applications for another company")
    void findByJob_wrongCompany() {
        JobListing otherCompanyJob = sampleJob(otherCompany, JobStatus.OPEN);
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(jobListingService.findJobListingById(otherCompanyJob.getId())).thenReturn(otherCompanyJob);

        assertThatThrownBy(() -> applicationService.findByJob(otherCompanyJob.getId(), manager, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("your company");

        verify(applicationRepository, never()).findAllByJobListing_IdAndCompanyId(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject listing job applications when manager has no company")
    void findByJob_managerWithoutCompany() {
        manager.setCompany(null);
        when(userService.findUserById(manager.getId())).thenReturn(manager);

        assertThatThrownBy(() -> applicationService.findByJob(job.getId(), manager, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("belong to a company");
    }

    @Test
    @DisplayName("Should reject an application when screening score is below the reject threshold")
    void processScreeningCompleted_rejectedBelowThreshold() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ScreeningCompletedEvent event = screeningCompleted(35);
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        applicationService.processScreeningCompleted(event);

        assertThat(application.getStage()).isEqualTo(ApplicationStage.REJECTED);
        assertThat(application.getScreeningResult().getMatchPercentage()).isEqualTo(35);
        assertThat(application.getScreeningResult().getMatchedSkills()).containsExactly("Java");
        assertThat(application.getStageUpdates()).hasSize(1);
        assertThat(application.getStageUpdates().getFirst().getCurrentStage()).isEqualTo(ApplicationStage.REJECTED);
        verify(applicationRepository).save(application);
    }

    @Test
    @DisplayName("Should advance an application when screening score reaches the pass threshold")
    void processScreeningCompleted_passThreshold() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ScreeningCompletedEvent event = screeningCompleted(75);
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        applicationService.processScreeningCompleted(event);

        assertThat(application.getStage()).isEqualTo(ApplicationStage.INTERVIEW_SCHEDULED);
        assertThat(application.getStageUpdates()).hasSize(1);
        assertThat(application.getStageUpdates().getFirst().getPreviousStage()).isEqualTo(ApplicationStage.SCREENING);
    }

    @Test
    @DisplayName("Should keep an application in screening when score is between thresholds")
    void processScreeningCompleted_middleScore() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ScreeningCompletedEvent event = screeningCompleted(50);
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        applicationService.processScreeningCompleted(event);

        assertThat(application.getStage()).isEqualTo(ApplicationStage.SCREENING);
        assertThat(application.getStageUpdates()).isEmpty();
        assertThat(application.getScreeningResult().getUnmatchedSkills()).containsExactly("Kafka");
    }

    @Test
    @DisplayName("Should treat null screening score as zero")
    void processScreeningCompleted_nullScore() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ScreeningCompletedEvent event = new ScreeningCompletedEvent(
                application.getId(),
                null,
                null,
                null,
                "Provider returned no score"
        );
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        applicationService.processScreeningCompleted(event);

        assertThat(application.getStage()).isEqualTo(ApplicationStage.REJECTED);
        assertThat(application.getScreeningResult().getMatchPercentage()).isNull();
        assertThat(application.getScreeningResult().getMatchedSkills()).isEmpty();
        assertThat(application.getScreeningResult().getUnmatchedSkills()).isEmpty();
    }

    @Test
    @DisplayName("Should update existing AI screening result instead of replacing the application")
    void processScreeningCompleted_existingResult() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        AiScreeningResult existing = new AiScreeningResult();
        existing.setId("result-1");
        existing.setApplication(application);
        existing.setMatchPercentage(20);

        ScreeningCompletedEvent event = screeningCompleted(80);
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.of(existing));

        applicationService.processScreeningCompleted(event);

        assertThat(application.getScreeningResult()).isSameAs(existing);
        assertThat(existing.getMatchPercentage()).isEqualTo(80);
        assertThat(application.getStage()).isEqualTo(ApplicationStage.INTERVIEW_SCHEDULED);
        verify(applicationRepository).save(application);
    }

    @Test
    @DisplayName("Should return not found when processing screening for an unknown application")
    void processScreeningCompleted_applicationNotFound() {
        ScreeningCompletedEvent event = screeningCompleted(80);
        when(applicationRepository.findById(event.getApplicationId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.processScreeningCompleted(event))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should wrap unexpected screening persistence failures")
    void processScreeningCompleted_unexpectedFailure() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ScreeningCompletedEvent event = screeningCompleted(80);
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());
        when(applicationRepository.save(application)).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> applicationService.processScreeningCompleted(event))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Failed to process AI screening result");
    }

    private Application sampleApplication(ApplicationStage stage) {
        Application application = new Application();
        application.setId("application-1");
        application.setApplicant(applicant);
        application.setJobListing(job);
        application.setResumeProfile(resumeProfile);
        application.setCompanyId(company.getId());
        application.setStage(stage);
        return application;
    }

    private ScreeningCompletedEvent screeningCompleted(Integer matchPercentage) {
        return new ScreeningCompletedEvent(
                "application-1",
                matchPercentage,
                List.of("Java"),
                List.of("Kafka"),
                "Matched Java but missed Kafka"
        );
    }

    private JobListing sampleJob(Company owner, JobStatus status) {
        JobListing listing = new JobListing();
        listing.setId(owner.getId().equals("company-1") ? "job-1" : "job-2");
        listing.setTitle("Backend Engineer");
        listing.setType(JobType.FULL_TIME);
        listing.setLocation("Remote");
        listing.setSummary("Build APIs");
        listing.setResponsibilities("Own backend services");
        listing.setRequiredQualifications("Java and Kafka");
        listing.setPreferredQualifications("Cloud experience");
        listing.setStatus(status);
        listing.setAutoRejectThreshold(40);
        listing.setAutoPassThreshold(75);
        listing.setCompany(owner);
        listing.getSkills().add(new JobListingSkill(listing, skill("Java")));
        listing.getSkills().add(new JobListingSkill(listing, skill("Kafka")));
        return listing;
    }

    private ResumeProfile sampleResumeProfile(User owner) {
        ResumeProfile profile = new ResumeProfile();
        profile.setId("resume-1");
        profile.setUser(owner);
        profile.setSummary("Backend engineer with Java experience");
        profile.setResumePdfUrl("https://cdn.example.com/resume.pdf");
        profile.getSkills().add(new ResumeProfileSkill(profile, skill("Java")));
        return profile;
    }

    private Skill skill(String name) {
        Skill skill = new Skill();
        skill.setId("skill-" + name.toLowerCase());
        skill.setName(name);
        return skill;
    }
}
