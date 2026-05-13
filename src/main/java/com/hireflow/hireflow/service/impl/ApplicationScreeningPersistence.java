package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.AiScreeningResult;
import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.repository.AiScreeningResultRepository;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.enums.ScreeningRecommendation;
import com.hireflow.hireflow.event.events.InconsistencyReviewCompletedEvent;
import com.hireflow.hireflow.event.events.ProjectConsistencyCompletedEvent;
import com.hireflow.hireflow.event.events.ResumeAnalysisCompletedEvent;
import com.hireflow.hireflow.event.events.ScreeningCompletedEvent;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class ApplicationScreeningPersistence {

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

    /**
     * Persists the final AI screening output. The application stage is NOT changed here —
     * stage transitions are an admin/HR responsibility (single or bulk endpoints).
     * The threshold-derived recommendation is stored so HR can filter applicants by it.
     */
    @Transactional
    public void finalizeScreening(ScreeningCompletedEvent event) {
        Application application = loadApplication(event.getApplicationId());
        AiScreeningResult result = loadOrCreate(application);

        if (event.getMatchPercentage() != null) result.setMatchPercentage(event.getMatchPercentage());
        if (event.getMatchedSkills() != null) result.setMatchedSkills(new ArrayList<>(event.getMatchedSkills()));
        if (event.getUnmatchedSkills() != null) result.setUnmatchedSkills(new ArrayList<>(event.getUnmatchedSkills()));
        if (event.getAiNarrativeSummary() != null) result.setAiNarrativeSummary(event.getAiNarrativeSummary());

        result.setRecommendation(resolveRecommendation(application.getJobListing(), result.getMatchPercentage()));

        application.setScreeningResult(result);
        applicationRepository.save(application);
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
        if (result.getRecommendation() == null) result.setRecommendation(ScreeningRecommendation.PENDING);
        return result;
    }

    private ScreeningRecommendation resolveRecommendation(JobListing job, Integer matchPercentage) {
        if (matchPercentage == null) {
            return ScreeningRecommendation.PENDING;
        }
        if (matchPercentage >= job.getAutoPassThreshold()) {
            return ScreeningRecommendation.AUTO_PASS;
        }
        if (matchPercentage < job.getAutoRejectThreshold()) {
            return ScreeningRecommendation.AUTO_REJECT;
        }
        return ScreeningRecommendation.MANUAL_REVIEW;
    }
}
