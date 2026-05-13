package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.dto.request.ApplyToJobRequest;
import com.hireflow.hireflow.dto.request.BulkStageUpdateRequest;
import com.hireflow.hireflow.dto.request.StageUpdateRequest;
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.dto.response.BulkStageUpdateResponse;
import com.hireflow.hireflow.dto.response.BulkStageUpdateResponse.BulkStageUpdateFailure;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.enums.ScreeningRecommendation;
import com.hireflow.hireflow.event.events.EmailNotificationEvent;
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
import com.hireflow.hireflow.service.ApplicationService;
import com.hireflow.hireflow.service.JobListingService;
import com.hireflow.hireflow.service.UserService;
import com.hireflow.hireflow.service.result.ApplicationSubmissionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserService userService;
    private final JobListingService jobListingService;
    private final AiScreeningEventProducer aiScreeningEventProducer;
    private final NotificationEventProducer notificationEventProducer;
    private final ApplicationMapper applicationMapper;
    private final ApplicationSubmissionPersistence applicationSubmissionPersistence;
    private final ApplicationScreeningPersistence applicationScreeningPersistence;
    private final ApplicationStageUpdatePersistence applicationStageUpdatePersistence;

    @Override
    public ApplicationResponse applyToJob(String jobId, ApplyToJobRequest request, User user) {
        try {
            ApplicationSubmissionResult result = applicationSubmissionPersistence.submit(jobId, request, user);
            aiScreeningEventProducer.publishApplicationSubmittedAsync(result.getEvent());
            publishSubmissionNotifications(result.getResponse());
            return result.getResponse();
        } catch (AccessDeniedException | DuplicateResourceException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Application submission failed: {}", ex.getMessage());
            throw new CustomException("Application submission failed: Internal Server Error");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> findMyApplications(User user, Pageable pageable) {
        User applicant = requireApplicant(user);
        return applicationRepository.findAllByApplicant_Id(applicant.getId(), pageable)
                .map(applicationMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse findMyApplication(String applicationId, User user) {
        User refreshed = requireAuthenticatedUser(user);
        Application application = switch (refreshed.getRole()) {
            case APPLICANT -> applicationRepository.findByIdAndApplicant_Id(applicationId, refreshed.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
            case ADMIN, HMANAGER -> {
                User manager = requireCompanyManager(refreshed);
                yield applicationRepository.findByIdAndCompanyId(applicationId, manager.getCompany().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
            }
        };
        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> findByJob(String jobId, ScreeningRecommendation recommendation, User user, Pageable pageable) {
        User manager = requireCompanyManager(user);
        JobListing job = jobListingService.findJobListingById(jobId);
        if (!manager.getCompany().getId().equals(job.getCompany().getId())) {
            throw new AccessDeniedException("You can only view applications for your company");
        }

        Page<Application> applications = recommendation == null
                ? applicationRepository.findAllByJobListing_IdAndCompanyId(jobId, manager.getCompany().getId(), pageable)
                : applicationRepository.findAllByJobListing_IdAndCompanyIdAndScreeningResult_Recommendation(
                jobId, manager.getCompany().getId(), recommendation, pageable);

        return applications.map(applicationMapper::toSummaryResponse);
    }

    @Override
    public ApplicationResponse updateApplicationStage(String applicationId, StageUpdateRequest request, User user) {
        try {
            User manager = requireCompanyManager(user);
            EmailNotificationEvent notification = applicationStageUpdatePersistence.updateStage(
                    applicationId, request.getTargetStage(), request.getReason(), manager);
            notificationEventProducer.publishApplicationStageUpdateAsync(notification);
            Application application = applicationRepository.findByIdAndCompanyId(applicationId, manager.getCompany().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
            return applicationMapper.toResponse(application);
        } catch (AccessDeniedException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Stage update failed for application {}: {}", applicationId, ex.getMessage());
            throw new CustomException("Stage update failed: Internal Server Error");
        }
    }

    @Override
    public BulkStageUpdateResponse bulkUpdateApplicationStage(BulkStageUpdateRequest request, User user) {
        User manager = requireCompanyManager(user);

        List<String> updated = new ArrayList<>();
        List<BulkStageUpdateFailure> failures = new ArrayList<>();
        List<EmailNotificationEvent> pendingNotifications = new ArrayList<>();

        for (String applicationId : request.getApplicationIds()) {
            try {
                EmailNotificationEvent notification = applicationStageUpdatePersistence.updateStage(
                        applicationId, request.getTargetStage(), request.getReason(), manager);
                pendingNotifications.add(notification);
                updated.add(applicationId);
            } catch (AccessDeniedException | ResourceNotFoundException | CustomException ex) {
                log.warn("Bulk stage update skipped {}: {}", applicationId, ex.getMessage());
                failures.add(new BulkStageUpdateFailure(applicationId, ex.getMessage()));
            } catch (Exception ex) {
                log.error("Bulk stage update failed for {}: {}", applicationId, ex.getMessage());
                failures.add(new BulkStageUpdateFailure(applicationId, "Internal Server Error"));
            }
        }

        for (EmailNotificationEvent notification : pendingNotifications) {
            notificationEventProducer.publishApplicationStageUpdateAsync(notification);
        }

        return new BulkStageUpdateResponse(
                request.getApplicationIds().size(),
                updated.size(),
                failures.size(),
                updated,
                failures
        );
    }

    @Override
    public void processResumeAnalysisCompleted(ResumeAnalysisCompletedEvent event) {
        runScreeningStage("resume analysis", event.getApplicationId(),
                () -> applicationScreeningPersistence.applyResumeAnalysis(event));
    }

    @Override
    public void processProjectConsistencyCompleted(ProjectConsistencyCompletedEvent event) {
        runScreeningStage("project consistency", event.getApplicationId(),
                () -> applicationScreeningPersistence.applyProjectConsistency(event));
    }

    @Override
    public void processInconsistencyReviewCompleted(InconsistencyReviewCompletedEvent event) {
        runScreeningStage("inconsistency review", event.getApplicationId(),
                () -> applicationScreeningPersistence.applyInconsistencyReview(event));
    }

    @Override
    public void processScreeningCompleted(ScreeningCompletedEvent event) {
        try {
            applicationScreeningPersistence.finalizeScreening(event);
            log.info("Persisted AI screening result for application {}; stage transition deferred to HR", event.getApplicationId());
        } catch (ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process AI screening result: {}", ex.getMessage());
            throw new CustomException("Failed to process AI screening result");
        }
    }

    private void runScreeningStage(String stageName, String applicationId, Runnable work) {
        try {
            work.run();
            log.info("Applied {} for application {}", stageName, applicationId);
        } catch (ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to apply {} for application {}: {}", stageName, applicationId, ex.getMessage());
            throw new CustomException("Failed to apply " + stageName + " result");
        }
    }

    private void publishSubmissionNotifications(ApplicationResponse response) {
        notificationEventProducer.publishApplicationStageUpdateAsync(EmailNotificationEvent.applicationStageUpdated(
                response.getApplicantEmail(),
                response.getId(),
                response.getApplicantId(),
                response.getJobListingId(),
                response.getJobTitle(),
                response.getCompanyId(),
                response.getCompanyName(),
                null,
                "APPLIED",
                "Application submitted",
                response.getApplicantEmail(),
                "Your application has been submitted."
        ));

        notificationEventProducer.publishApplicationStageUpdateAsync(EmailNotificationEvent.applicationStageUpdated(
                response.getApplicantEmail(),
                response.getId(),
                response.getApplicantId(),
                response.getJobListingId(),
                response.getJobTitle(),
                response.getCompanyId(),
                response.getCompanyName(),
                "APPLIED",
                "SCREENING",
                "Queued for AI screening",
                "system",
                "Your application is now in AI screening."
        ));
    }

    private User requireApplicant(User user) {
        User refreshed = requireAuthenticatedUser(user);
        if (refreshed == null || refreshed.getRole() != Role.APPLICANT) {
            throw new AccessDeniedException("Only applicants can apply to jobs");
        }
        return refreshed;
    }

    private User requireCompanyManager(User user) {
        User refreshed = requireAuthenticatedUser(user);
        if (refreshed == null || (refreshed.getRole() != Role.ADMIN && refreshed.getRole() != Role.HMANAGER)) {
            throw new AccessDeniedException("Only admins and hiring managers can manage job applications");
        }
        if (refreshed.getCompany() == null) {
            throw new AccessDeniedException("You must belong to a company to manage job applications");
        }
        return refreshed;
    }

    private User requireAuthenticatedUser(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        User refreshed = userService.findUserById(user.getId());
        if (refreshed == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return refreshed;
    }
}
