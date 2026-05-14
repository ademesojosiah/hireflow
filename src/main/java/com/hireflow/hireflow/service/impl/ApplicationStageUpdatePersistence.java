package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ApplicationStageUpdatePersistence {

    private final ApplicationRepository applicationRepository;

    @Transactional
    public EmailNotificationEvent updateStage(String applicationId, ApplicationStage targetStage, String reason, User actor) {
        Application application = applicationRepository.findByIdAndCompanyId(applicationId, actor.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        ApplicationStage previousStage = application.getStage();
        if (!ApplicationStageTransitions.isAllowed(previousStage, targetStage)) {
            throw new CustomException("Stage transition not allowed: " + previousStage + " to " + targetStage);
        }

        String effectiveReason = (reason == null || reason.isBlank())
                ? "Stage updated by " + actor.getEmail()
                : reason;

        application.setStage(targetStage);
        application.addStageUpdate(previousStage, targetStage, effectiveReason, actor.getEmail());
        applicationRepository.save(application);

        return toNotificationEvent(application, previousStage, targetStage, effectiveReason, actor);
    }

    private EmailNotificationEvent toNotificationEvent(
            Application application,
            ApplicationStage previousStage,
            ApplicationStage currentStage,
            String reason,
            User actor
    ) {
        User applicant = application.getApplicant();
        JobListing job = application.getJobListing();

        if (applicant == null) {
            throw new AccessDeniedException("Application is missing an applicant");
        }
        // The notification service routes the email by `to` and the SSE push by `applicantId`.
        // If either is missing the applicant cannot be reached - fail loudly rather than publish
        // a half-formed event that will silently fail downstream.
        if (applicant.getEmail() == null || applicant.getEmail().isBlank()) {
            throw new CustomException("Applicant " + applicant.getId() + " has no email - cannot publish stage-change notification");
        }
        if (applicant.getId() == null || applicant.getId().isBlank()) {
            throw new CustomException("Applicant is missing an id - cannot route stage-change SSE event");
        }

        return EmailNotificationEvent.applicationStageUpdated(
                applicant.getEmail(),
                application.getId(),
                applicant.getId(),
                job == null ? null : job.getId(),
                job == null ? null : job.getTitle(),
                application.getCompanyId(),
                job == null || job.getCompany() == null ? null : job.getCompany().getName(),
                previousStage == null ? null : previousStage.name(),
                currentStage.name(),
                reason,
                actor.getEmail(),
                candidateMessage(currentStage)
        );
    }

    private String candidateMessage(ApplicationStage currentStage) {
        return switch (currentStage) {
            case INTERVIEW_SCHEDULED -> "Your application has moved to interview scheduling.";
            case REJECTED -> "Your application status has been updated by the hiring team.";
            case SCREENING -> "Your application is in screening.";
            case OFFER_SENT -> "You have received an offer update for your application.";
            case HIRED -> "Welcome aboard - your application has moved to hired.";
            case APPLIED -> "Your application has been submitted.";
        };
    }
}
