package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.AiScreeningResult;
import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.ApplicationAnswer;
import com.hireflow.hireflow.data.model.StageUpdate;
import com.hireflow.hireflow.dto.response.AiScreeningResultResponse;
import com.hireflow.hireflow.dto.response.AiScreeningStageResponse;
import com.hireflow.hireflow.dto.response.ApplicationAnswerResponse;
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.dto.response.StageUpdateResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setStage(application.getStage());
        response.setCompanyId(application.getCompanyId());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());

        if (application.getApplicant() != null) {
            response.setApplicantId(application.getApplicant().getId());
            response.setApplicantEmail(application.getApplicant().getEmail());
            response.setApplicantName(application.getApplicant().getFirstName() + " " + application.getApplicant().getLastName());
        }

        if (application.getJobListing() != null) {
            response.setJobListingId(application.getJobListing().getId());
            response.setJobTitle(application.getJobListing().getTitle());
            if (application.getJobListing().getCompany() != null) {
                response.setCompanyName(application.getJobListing().getCompany().getName());
            }
        }

        response.setScreeningResult(toScreeningResponse(application.getScreeningResult()));
        response.setStageUpdates(application.getStageUpdates() == null ? List.of()
                : application.getStageUpdates().stream()
                .sorted(Comparator.comparing(StageUpdate::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toStageUpdateResponse)
                .toList());
        response.setAnswers(application.getAnswers() == null ? List.of()
                : application.getAnswers().stream()
                .sorted(Comparator.comparing(ApplicationAnswer::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toAnswerResponse)
                .toList());
        return response;
    }

    private ApplicationAnswerResponse toAnswerResponse(ApplicationAnswer answer) {
        return new ApplicationAnswerResponse(
                answer.getId(),
                answer.getJobQuestion() == null ? null : answer.getJobQuestion().getId(),
                answer.getQuestionSnapshot(),
                answer.getAnswer(),
                answer.getCreatedAt()
        );
    }

    private AiScreeningResultResponse toScreeningResponse(AiScreeningResult result) {
        if (result == null) {
            return null;
        }

        return new AiScreeningResultResponse(
                result.getId(),
                result.getMatchPercentage(),
                result.getMatchedSkills(),
                result.getUnmatchedSkills(),
                result.getAiNarrativeSummary(),
                new AiScreeningStageResponse(
                        result.getResumeAnalysisScore(),
                        result.getResumeAnalysisExplanation(),
                        result.getResumeAnalysisReview()
                ),
                new AiScreeningStageResponse(
                        result.getProjectConsistencyScore(),
                        result.getProjectConsistencyExplanation(),
                        result.getProjectConsistencyReview()
                ),
                new AiScreeningStageResponse(
                        result.getInconsistencyScore(),
                        result.getInconsistencyExplanation(),
                        result.getInconsistencyReview()
                ),
                result.getInconsistencySeverity(),
                result.getRecommendedHumanReviewAction(),
                result.getCreatedAt()
        );
    }

    private StageUpdateResponse toStageUpdateResponse(StageUpdate update) {
        return new StageUpdateResponse(
                update.getId(),
                update.getPreviousStage(),
                update.getCurrentStage(),
                update.getReason(),
                update.getActor(),
                update.getCreatedAt()
        );
    }
}
