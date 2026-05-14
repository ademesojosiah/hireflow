package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.Scorecard;
import com.hireflow.hireflow.data.model.ScorecardScore;
import com.hireflow.hireflow.dto.response.ScorecardResponse;
import com.hireflow.hireflow.dto.response.ScorecardScoreResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScorecardMapper {

    public ScorecardResponse toResponse(Scorecard scorecard) {
        if (scorecard == null) return null;
        List<ScorecardScoreResponse> scores = scorecard.getScores() == null ? List.of()
                : scorecard.getScores().stream().map(this::toScoreResponse).toList();

        return new ScorecardResponse(
                scorecard.getId(),
                scorecard.getInterviewSlot() == null ? null : scorecard.getInterviewSlot().getId(),
                scorecard.getApplication() == null ? null : scorecard.getApplication().getId(),
                scorecard.getTemplate() == null ? null : scorecard.getTemplate().getId(),
                scorecard.getTemplateNameSnapshot(),
                scorecard.getInterviewerEmail(),
                scorecard.getSubmittedById(),
                scorecard.getSubmittedByEmail(),
                scorecard.getSubmittedByRole(),
                scorecard.getOverallNotes(),
                scorecard.getTotalScore(),
                scorecard.getMaxPossibleScore(),
                scorecard.getStatus(),
                scorecard.getSubmittedAt(),
                scores,
                scorecard.getCreatedAt(),
                scorecard.getUpdatedAt()
        );
    }

    private ScorecardScoreResponse toScoreResponse(ScorecardScore score) {
        return new ScorecardScoreResponse(
                score.getId(),
                score.getCriterion() == null ? null : score.getCriterion().getId(),
                score.getCriterionCategorySnapshot(),
                score.getCriterionNameSnapshot(),
                score.getMaxScoreSnapshot(),
                score.getScore(),
                score.getNotes()
        );
    }
}
