package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.AiScreeningResult;
import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.JobListingSkill;
import com.hireflow.hireflow.data.model.JobQuestion;
import com.hireflow.hireflow.data.model.ResumeProfile;
import com.hireflow.hireflow.data.model.ResumeProfileSkill;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.AiScreeningResultRepository;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.dto.request.ApplicationAnswerRequest;
import com.hireflow.hireflow.dto.request.ApplyToJobRequest;
import com.hireflow.hireflow.dto.request.BulkStageUpdateRequest;
import com.hireflow.hireflow.dto.request.StageUpdateRequest;
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.dto.response.BulkStageUpdateResponse;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.JobType;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.enums.ScreeningRecommendation;
import com.hireflow.hireflow.event.events.InconsistencyReviewCompletedEvent;
import com.hireflow.hireflow.event.events.ProjectConsistencyCompletedEvent;
import com.hireflow.hireflow.event.events.ResumeAnalysisCompletedEvent;
import com.hireflow.hireflow.event.events.ScreeningCompletedEvent;
import com.hireflow.hireflow.event.producer.AiScreeningEventProducer;
import com.hireflow.hireflow.event.producer.NotificationEventProducer;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.ApplicationMapper;
import com.hireflow.hireflow.service.impl.ApplicationScreeningPersistence;
import com.hireflow.hireflow.service.impl.ApplicationServiceImpl;
import com.hireflow.hireflow.service.impl.ApplicationStageUpdatePersistence;
import com.hireflow.hireflow.service.impl.ApplicationSubmissionPersistence;
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
    @Mock private NotificationEventProducer notificationEventProducer;
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
        ApplicationScreeningPersistence applicationScreeningPersistence = new ApplicationScreeningPersistence(
                applicationRepository,
                aiScreeningResultRepository
        );
        ApplicationStageUpdatePersistence applicationStageUpdatePersistence = new ApplicationStageUpdatePersistence(
                applicationRepository
        );
        applicationService = new ApplicationServiceImpl(
                applicationRepository,
                userService,
                jobListingService,
                aiScreeningEventProducer,
                notificationEventProducer,
                applicationMapper,
                applicationSubmissionPersistence,
                applicationScreeningPersistence,
                applicationStageUpdatePersistence
        );
    }

    // ---------- applyToJob ----------

    @Test
    @DisplayName("Should submit application, save SCREENING stage, and publish AI screening event without answers")
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

        ApplicationResponse response = applicationService.applyToJob(job.getId(), answerRequest(), applicant);

        assertThat(response).isSameAs(expected);
        assertThat(savedApplication.get().getStage()).isEqualTo(ApplicationStage.SCREENING);
        assertThat(savedApplication.get().getStageUpdates())
                .extracting("currentStage")
                .containsExactly(ApplicationStage.APPLIED, ApplicationStage.SCREENING);

        verify(aiScreeningEventProducer).publishApplicationSubmittedAsync(argThat(event ->
                event != null
                        && "application-1".equals(event.getApplicationId())
                        && "applicant-1".equals(event.getApplicantId())
                        && event.getJobSkills().equals(List.of("Java", "Kafka"))
                        && event.getApplicantSkills().equals(List.of("Java"))
        ));
    }

    @Test
    @DisplayName("Should persist applicant answers on the Application but never include them in the Kafka event")
    void applyToJob_persistsAnswersLocallyOnly() {
        JobQuestion technicalQuestion = question("question-1", "Describe a Kafka project.", "Mentions Kafka consumer/producer.");
        job.getQuestions().add(technicalQuestion);

        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(resumeProfileService.findProfileByUserId(applicant.getId())).thenReturn(resumeProfile);
        when(applicationRepository.existsByApplicant_IdAndJobListing_Id(applicant.getId(), job.getId())).thenReturn(false);
        AtomicReference<Application> saved = new AtomicReference<>();
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application app = invocation.getArgument(0);
            app.setId("application-1");
            saved.set(app);
            return app;
        });
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

        ApplyToJobRequest request = answerRequest(new ApplicationAnswerRequest(
                "question-1",
                "Built a Kafka pipeline with consumers and producers in production."
        ));

        applicationService.applyToJob(job.getId(), request, applicant);

        assertThat(saved.get().getAnswers()).hasSize(1);
        assertThat(saved.get().getAnswers().getFirst().getAnswer())
                .isEqualTo("Built a Kafka pipeline with consumers and producers in production.");

        // The Kafka event must NOT contain answers — Q&A is reserved for human review.
        verify(aiScreeningEventProducer).publishApplicationSubmittedAsync(argThat(event ->
                event != null && "application-1".equals(event.getApplicationId())
        ));
    }

    @Test
    @DisplayName("Should reject application when a required job question has no answer")
    void applyToJob_missingAnswer() {
        job.getQuestions().add(question("question-1", "Describe a Kafka project.", "Mentions Kafka."));

        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(resumeProfileService.findProfileByUserId(applicant.getId())).thenReturn(resumeProfile);
        when(applicationRepository.existsByApplicant_IdAndJobListing_Id(applicant.getId(), job.getId())).thenReturn(false);

        assertThatThrownBy(() -> applicationService.applyToJob(job.getId(), answerRequest(), applicant))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Answer required for question");

        verify(applicationRepository, never()).save(any());
        verify(aiScreeningEventProducer, never()).publishApplicationSubmittedAsync(any());
    }

    @Test
    @DisplayName("Should reject application submission from a non-applicant")
    void applyToJob_forbiddenForNonApplicant() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);

        assertThatThrownBy(() -> applicationService.applyToJob(job.getId(), answerRequest(), manager))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only applicants");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject duplicate applications for the same applicant and job")
    void applyToJob_duplicateApplication() {
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(resumeProfileService.findProfileByUserId(applicant.getId())).thenReturn(resumeProfile);
        when(applicationRepository.existsByApplicant_IdAndJobListing_Id(applicant.getId(), job.getId())).thenReturn(true);

        assertThatThrownBy(() -> applicationService.applyToJob(job.getId(), answerRequest(), applicant))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    // ---------- findByJob recommendation filter ----------

    @Test
    @DisplayName("Should list all applications for a job when no recommendation filter is provided")
    void findByJob_noFilter() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        PageRequest pageable = PageRequest.of(0, 5);

        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(applicationRepository.findAllByJobListing_IdAndCompanyId(job.getId(), company.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(applicationMapper.toSummaryResponse(application)).thenReturn(new ApplicationResponse());

        Page<ApplicationResponse> response = applicationService.findByJob(job.getId(), null, manager, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(applicationRepository).findAllByJobListing_IdAndCompanyId(job.getId(), company.getId(), pageable);
    }

    @Test
    @DisplayName("Should filter applications by screening recommendation when provided")
    void findByJob_filteredByRecommendation() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        PageRequest pageable = PageRequest.of(0, 5);

        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(jobListingService.findJobListingById(job.getId())).thenReturn(job);
        when(applicationRepository.findAllByJobListing_IdAndCompanyIdAndScreeningResult_Recommendation(
                job.getId(), company.getId(), ScreeningRecommendation.AUTO_PASS, pageable
        )).thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(applicationMapper.toSummaryResponse(application)).thenReturn(new ApplicationResponse());

        Page<ApplicationResponse> response = applicationService.findByJob(
                job.getId(), ScreeningRecommendation.AUTO_PASS, manager, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(applicationRepository).findAllByJobListing_IdAndCompanyIdAndScreeningResult_Recommendation(
                job.getId(), company.getId(), ScreeningRecommendation.AUTO_PASS, pageable);
    }

    @Test
    @DisplayName("Should block managers from viewing applications for another company's job")
    void findByJob_wrongCompany() {
        JobListing otherCompanyJob = sampleJob(otherCompany, JobStatus.OPEN);
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(jobListingService.findJobListingById(otherCompanyJob.getId())).thenReturn(otherCompanyJob);

        assertThatThrownBy(() -> applicationService.findByJob(otherCompanyJob.getId(), null, manager, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("your company");
    }

    // ---------- AI screening result persistence (no auto-advance) ----------

    @Test
    @DisplayName("Should persist screening result with AUTO_REJECT recommendation but NOT change stage when score is below reject threshold")
    void processScreeningCompleted_belowRejectStaysInScreening() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ScreeningCompletedEvent event = screeningCompleted(35);
        when(applicationRepository.findByIdForUpdate(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        applicationService.processScreeningCompleted(event);

        assertThat(application.getStage()).isEqualTo(ApplicationStage.SCREENING);
        assertThat(application.getStageUpdates()).isEmpty();
        assertThat(application.getScreeningResult().getMatchPercentage()).isEqualTo(35);
        assertThat(application.getScreeningResult().getRecommendation()).isEqualTo(ScreeningRecommendation.AUTO_REJECT);
        verifyNoInteractions(notificationEventProducer);
    }

    @Test
    @DisplayName("Should persist screening result with AUTO_PASS recommendation but NOT change stage when score is above pass threshold")
    void processScreeningCompleted_abovePassStaysInScreening() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ScreeningCompletedEvent event = screeningCompleted(85);
        when(applicationRepository.findByIdForUpdate(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        applicationService.processScreeningCompleted(event);

        assertThat(application.getStage()).isEqualTo(ApplicationStage.SCREENING);
        assertThat(application.getStageUpdates()).isEmpty();
        assertThat(application.getScreeningResult().getRecommendation()).isEqualTo(ScreeningRecommendation.AUTO_PASS);
        verifyNoInteractions(notificationEventProducer);
    }

    @Test
    @DisplayName("Should persist screening result with MANUAL_REVIEW recommendation when score is between thresholds")
    void processScreeningCompleted_middleScoreManualReview() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ScreeningCompletedEvent event = screeningCompleted(55);
        when(applicationRepository.findByIdForUpdate(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        applicationService.processScreeningCompleted(event);

        assertThat(application.getStage()).isEqualTo(ApplicationStage.SCREENING);
        assertThat(application.getScreeningResult().getRecommendation()).isEqualTo(ScreeningRecommendation.MANUAL_REVIEW);
        verifyNoInteractions(notificationEventProducer);
    }

    @Test
    @DisplayName("Should persist PENDING recommendation when match percentage is null")
    void processScreeningCompleted_nullScoreIsPending() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ScreeningCompletedEvent event = new ScreeningCompletedEvent(
                application.getId(), null, null, null, "Provider returned no score"
        );
        when(applicationRepository.findByIdForUpdate(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        applicationService.processScreeningCompleted(event);

        assertThat(application.getStage()).isEqualTo(ApplicationStage.SCREENING);
        assertThat(application.getScreeningResult().getRecommendation()).isEqualTo(ScreeningRecommendation.PENDING);
    }

    @Test
    @DisplayName("Should apply resume analysis without moving the stage and without notifying")
    void processResumeAnalysisCompleted_partialMerge() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        when(applicationRepository.findByIdForUpdate(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        ResumeAnalysisCompletedEvent event = new ResumeAnalysisCompletedEvent(
                application.getId(), 72, "Strong Java alignment", "Resume mostly matches required skills."
        );

        applicationService.processResumeAnalysisCompleted(event);

        AiScreeningResult result = application.getScreeningResult();
        assertThat(result.getResumeAnalysisScore()).isEqualTo(72);
        assertThat(application.getStage()).isEqualTo(ApplicationStage.SCREENING);
        verifyNoInteractions(notificationEventProducer);
    }

    @Test
    @DisplayName("Should apply project consistency without overwriting other stages")
    void processProjectConsistencyCompleted_partialMerge() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        AiScreeningResult existing = new AiScreeningResult();
        existing.setApplication(application);
        existing.setResumeAnalysisScore(80);
        when(applicationRepository.findByIdForUpdate(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.of(existing));

        ProjectConsistencyCompletedEvent event = new ProjectConsistencyCompletedEvent(
                application.getId(), 65, "Resume mentions Java and Kafka", "Resume needs human review."
        );

        applicationService.processProjectConsistencyCompleted(event);

        assertThat(existing.getProjectConsistencyScore()).isEqualTo(65);
        assertThat(existing.getResumeAnalysisScore()).isEqualTo(80);
        verifyNoInteractions(notificationEventProducer);
    }

    @Test
    @DisplayName("Should apply inconsistency review without changing stage")
    void processInconsistencyReviewCompleted_partialMerge() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        when(applicationRepository.findByIdForUpdate(application.getId())).thenReturn(Optional.of(application));
        when(aiScreeningResultRepository.findByApplication_Id(application.getId())).thenReturn(Optional.empty());

        InconsistencyReviewCompletedEvent event = new InconsistencyReviewCompletedEvent(
                application.getId(), 55, "MEDIUM",
                "Some claims are weakly supported.",
                "Review weak claims.",
                "Check the weaker claims during interview."
        );

        applicationService.processInconsistencyReviewCompleted(event);

        AiScreeningResult result = application.getScreeningResult();
        assertThat(result.getInconsistencyScore()).isEqualTo(55);
        assertThat(result.getInconsistencySeverity()).isEqualTo("MEDIUM");
        assertThat(application.getStage()).isEqualTo(ApplicationStage.SCREENING);
        verifyNoInteractions(notificationEventProducer);
    }

    // ---------- HR stage updates ----------

    @Test
    @DisplayName("Should let HR advance an application from SCREENING to INTERVIEW_SCHEDULED and publish a notification addressed to the applicant")
    void updateApplicationStage_screeningToInterview() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        ApplicationResponse mapped = new ApplicationResponse();

        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyId(application.getId(), company.getId()))
                .thenReturn(Optional.of(application));
        when(applicationMapper.toResponse(application)).thenReturn(mapped);

        StageUpdateRequest request = new StageUpdateRequest(ApplicationStage.INTERVIEW_SCHEDULED, "Looks strong");

        ApplicationResponse response = applicationService.updateApplicationStage(application.getId(), request, manager);

        assertThat(response).isSameAs(mapped);
        assertThat(application.getStage()).isEqualTo(ApplicationStage.INTERVIEW_SCHEDULED);
        assertThat(application.getStageUpdates()).hasSize(1);
        assertThat(application.getStageUpdates().getFirst().getCurrentStage()).isEqualTo(ApplicationStage.INTERVIEW_SCHEDULED);
        // The notification must carry the applicant's email (for the SMTP send) AND the applicant id
        // (which the notification service uses to route the SSE event to the right subscriber).
        verify(notificationEventProducer).publishApplicationStageUpdate(argThat(notification ->
                notification != null
                        && applicant.getEmail().equals(notification.getTo())
                        && applicant.getId().equals(notification.getApplicantId())
                        && application.getId().equals(notification.getApplicationId())
                        && "INTERVIEW_SCHEDULED".equals(notification.getCurrentStage())
                        && "SCREENING".equals(notification.getPreviousStage())
                        && manager.getEmail().equals(notification.getActor())
        ));
    }

    @Test
    @DisplayName("Should reject invalid stage transitions (e.g. SCREENING → HIRED)")
    void updateApplicationStage_invalidTransition() {
        Application application = sampleApplication(ApplicationStage.SCREENING);
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyId(application.getId(), company.getId()))
                .thenReturn(Optional.of(application));

        StageUpdateRequest request = new StageUpdateRequest(ApplicationStage.HIRED, "skip ahead");

        assertThatThrownBy(() -> applicationService.updateApplicationStage(application.getId(), request, manager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Stage transition not allowed");
        assertThat(application.getStage()).isEqualTo(ApplicationStage.SCREENING);
        verifyNoInteractions(notificationEventProducer);
    }

    @Test
    @DisplayName("Should bulk-advance multiple applications, report per-id results, and notify each successful applicant")
    void bulkUpdateApplicationStage_mixedResults() {
        Application app1 = sampleApplication(ApplicationStage.SCREENING);
        app1.setId("application-1");
        Application app2 = sampleApplication(ApplicationStage.HIRED);
        app2.setId("application-2");

        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyId("application-1", company.getId()))
                .thenReturn(Optional.of(app1));
        when(applicationRepository.findByIdAndCompanyId("application-2", company.getId()))
                .thenReturn(Optional.of(app2));
        when(applicationRepository.findByIdAndCompanyId("application-3", company.getId()))
                .thenReturn(Optional.empty());

        BulkStageUpdateRequest request = new BulkStageUpdateRequest(
                List.of("application-1", "application-2", "application-3"),
                ApplicationStage.REJECTED,
                "Closing remaining queue"
        );

        BulkStageUpdateResponse response = applicationService.bulkUpdateApplicationStage(request, manager);

        assertThat(response.getRequested()).isEqualTo(3);
        assertThat(response.getSucceeded()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(2);
        assertThat(response.getUpdatedApplicationIds()).containsExactly("application-1");
        assertThat(response.getFailures()).extracting("applicationId")
                .containsExactlyInAnyOrder("application-2", "application-3");
        // Exactly one notification fires (the only successful update), addressed to that applicant.
        verify(notificationEventProducer).publishApplicationStageUpdate(argThat(notification ->
                notification != null
                        && "application-1".equals(notification.getApplicationId())
                        && applicant.getEmail().equals(notification.getTo())
                        && applicant.getId().equals(notification.getApplicantId())
                        && "REJECTED".equals(notification.getCurrentStage())
        ));
    }

    @Test
    @DisplayName("Should publish bulk-update notifications strictly in the request order, one applicant after the other")
    void bulkUpdateApplicationStage_preservesPerApplicantOrder() {
        // Three different applicants so we can prove each notification carries its own routing fields.
        User ada = applicant;
        User bob = new User("Bob", "Builder", "bob@example.com", "password", Role.APPLICANT, true);
        bob.setId("applicant-2");
        User cara = new User("Cara", "Carter", "cara@example.com", "password", Role.APPLICANT, true);
        cara.setId("applicant-3");

        Application app1 = sampleApplication(ApplicationStage.SCREENING);
        app1.setId("application-1");
        app1.setApplicant(ada);

        Application app2 = sampleApplication(ApplicationStage.SCREENING);
        app2.setId("application-2");
        app2.setApplicant(bob);

        Application app3 = sampleApplication(ApplicationStage.SCREENING);
        app3.setId("application-3");
        app3.setApplicant(cara);

        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyId("application-1", company.getId())).thenReturn(Optional.of(app1));
        when(applicationRepository.findByIdAndCompanyId("application-2", company.getId())).thenReturn(Optional.of(app2));
        when(applicationRepository.findByIdAndCompanyId("application-3", company.getId())).thenReturn(Optional.of(app3));

        BulkStageUpdateRequest request = new BulkStageUpdateRequest(
                List.of("application-1", "application-2", "application-3"),
                ApplicationStage.INTERVIEW_SCHEDULED,
                "Batch advance"
        );

        BulkStageUpdateResponse response = applicationService.bulkUpdateApplicationStage(request, manager);

        assertThat(response.getSucceeded()).isEqualTo(3);
        assertThat(response.getUpdatedApplicationIds()).containsExactly("application-1", "application-2", "application-3");

        // Inorder verification: applicant 1 first, then 2, then 3 — each addressed to the right person.
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(notificationEventProducer);
        inOrder.verify(notificationEventProducer).publishApplicationStageUpdate(argThat(n ->
                n != null && "application-1".equals(n.getApplicationId())
                        && ada.getEmail().equals(n.getTo()) && ada.getId().equals(n.getApplicantId())));
        inOrder.verify(notificationEventProducer).publishApplicationStageUpdate(argThat(n ->
                n != null && "application-2".equals(n.getApplicationId())
                        && bob.getEmail().equals(n.getTo()) && bob.getId().equals(n.getApplicantId())));
        inOrder.verify(notificationEventProducer).publishApplicationStageUpdate(argThat(n ->
                n != null && "application-3".equals(n.getApplicationId())
                        && cara.getEmail().equals(n.getTo()) && cara.getId().equals(n.getApplicantId())));
    }

    @Test
    @DisplayName("Should require ADMIN or HMANAGER for stage updates")
    void updateApplicationStage_forbiddenForApplicant() {
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);

        StageUpdateRequest request = new StageUpdateRequest(ApplicationStage.INTERVIEW_SCHEDULED, null);

        assertThatThrownBy(() -> applicationService.updateApplicationStage("application-1", request, applicant))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("admins and hiring managers");
    }

    // ---------- helpers ----------

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

    private ApplyToJobRequest answerRequest(ApplicationAnswerRequest... answers) {
        ApplyToJobRequest request = new ApplyToJobRequest();
        request.setAnswers(new java.util.ArrayList<>(java.util.Arrays.asList(answers)));
        return request;
    }

    private JobQuestion question(String id, String text, String guide) {
        JobQuestion q = new JobQuestion();
        q.setId(id);
        q.setQuestion(text);
        q.setAnswer(guide);
        q.setJobListing(job);
        return q;
    }
}
