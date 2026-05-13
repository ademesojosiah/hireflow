package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.AiScreeningResult;
import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.AiScreeningResultRepository;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.event.events.InconsistencyReviewCompletedEvent;
import com.hireflow.hireflow.event.events.ProjectConsistencyCompletedEvent;
import com.hireflow.hireflow.event.events.ResumeAnalysisCompletedEvent;
import com.hireflow.hireflow.event.events.ScreeningCompletedEvent;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.service.result.ApplicationScreeningResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationScreeningPersistence {

    private static final String ACTOR = "ai-screening";

    private final ApplicationRepository applicationRepository;
    private final AiScreeningResultRepository aiScreeningResultRepository;

    @Transactional
    public void applyResumeAnalysis(ResumeAnalysisCompletedEvent event) {
        Application application = loadApplication(event.getApplicationId());
        AiScreeningResult result = loadOrCreate(application);

        if (event.getScore() != null) result.setResumeAnalysisScore(event.getScore());
        if (event.getExplanation() != null) result.setResumeAnalysisExplanation(event.getExplanation());
        if (event.getReview() != null) result.setResumeAnalysisReview(event.getReview());

        application.setScreeningResult(result);
        applicationRepository.save(application);
    }

    @Transactional
    public void applyProjectConsistency(ProjectConsistencyCompletedEvent event) {
        Application application = loadApplication(event.getApplicationId());
        AiScreeningResult result = loadOrCreate(application);

        if (event.getScore() != null) result.setProjectConsistencyScore(event.getScore());
        if (event.getExplanation() != null) result.setProjectConsistencyExplanation(event.getExplanation());
        if (event.getReview() != null) result.setProjectConsistencyReview(event.getReview());

        application.setScreeningResult(result);
        applicationRepository.save(application);
    }

    @Transactional
    public void applyInconsistencyReview(InconsistencyReviewCompletedEvent event) {
        Application application = loadApplication(event.getApplicationId());
        AiScreeningResult result = loadOrCreate(application);

        if (event.getScore() != null) result.setInconsistencyScore(event.getScore());
        if (event.getSeverity() != null) result.setInconsistencySeverity(event.getSeverity());
        if (event.getExplanation() != null) result.setInconsistencyExplanation(event.getExplanation());
        if (event.getReview() != null) result.setInconsistencyReview(event.getReview());
        if (event.getRecommendedHumanReviewAction() != null) {
            result.setRecommendedHumanReviewAction(event.getRecommendedHumanReviewAction());
        }

        application.setScreeningResult(result);
        applicationRepository.save(application);
    }

    @Transactional
    public ApplicationScreeningResult finalizeScreening(ScreeningCompletedEvent event) {
        Application application = loadApplication(event.getApplicationId());
        AiScreeningResult result = loadOrCreate(application);

        if (event.getMatchPercentage() != null) result.setMatchPercentage(event.getMatchPercentage());
        if (event.getMatchedSkills() != null) result.setMatchedSkills(new ArrayList<>(event.getMatchedSkills()));
        if (event.getUnmatchedSkills() != null) result.setUnmatchedSkills(new ArrayList<>(event.getUnmatchedSkills()));
        if (event.getAiNarrativeSummary() != null) result.setAiNarrativeSummary(event.getAiNarrativeSummary());

        application.setScreeningResult(result);

        ApplicationStage previousStage = application.getStage();
        ApplicationStage nextStage = resolveScreeningStage(application, event.getMatchPercentage());
        String reason = resolveReason(nextStage, previousStage);

        if (nextStage != previousStage) {
            application.setStage(nextStage);
            application.addStageUpdate(previousStage, nextStage, reason, ACTOR);
        }

        applicationRepository.save(application);

        return new ApplicationScreeningResult(toNotificationEvent(
                application,
                previousStage,
                application.getStage(),
                reason
        ));
    }

    private Application loadApplication(String applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private AiScreeningResult loadOrCreate(Application application) {
        AiScreeningResult result = aiScreeningResultRepository.findByApplication_Id(application.getId())
                .orElseGet(AiScreeningResult::new);
        result.setApplication(application);
        if (result.getMatchedSkills() == null) result.setMatchedSkills(new ArrayList<>());
        if (result.getUnmatchedSkills() == null) result.setUnmatchedSkills(new ArrayList<>());
        return result;
    }

    private ApplicationStage resolveScreeningStage(Application application, Integer matchPercentage) {
        int score = matchPercentage == null ? 0 : matchPercentage;
        JobListing job = application.getJobListing();

        if (score < job.getAutoRejectThreshold()) {
            return ApplicationStage.REJECTED;
        }
        if (score >= job.getAutoPassThreshold()) {
            return ApplicationStage.INTERVIEW_SCHEDULED;
        }
        return ApplicationStage.SCREENING;
    }

    private String resolveReason(ApplicationStage nextStage, ApplicationStage previousStage) {
        if (nextStage == previousStage) {
            return "AI screening completed; hiring team review required";
        }
        return "AI screening completed";
    }

    private EmailNotificationEvent toNotificationEvent(
            Application application,
            ApplicationStage previousStage,
            ApplicationStage currentStage,
            String reason
    ) {
        User applicant = application.getApplicant();
        JobListing job = application.getJobListing();

        return EmailNotificationEvent.applicationStageUpdated(
                applicant.getEmail(),
                application.getId(),
                applicant.getId(),
                job.getId(),
                job.getTitle(),
                application.getCompanyId(),
                job.getCompany() == null ? null : job.getCompany().getName(),
                previousStage == null ? null : previousStage.name(),
                currentStage.name(),
                reason,
                ACTOR,
                candidateMessage(currentStage)
        );
    }

    private String candidateMessage(ApplicationStage currentStage) {
        return switch (currentStage) {
            case INTERVIEW_SCHEDULED -> "Your application passed initial screening and is ready for interview scheduling.";
            case REJECTED -> "Your application screening has been completed. The hiring team will share the next update.";
            case SCREENING -> "Your application screening is complete and is under hiring team review.";
            case OFFER_SENT -> "An offer update is available for your application.";
            case HIRED -> "Your application has moved to hired.";
            case APPLIED -> "Your application has been submitted.";
        };
    }
}
