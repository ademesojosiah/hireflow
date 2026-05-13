package com.hireflow.hireflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiScreeningResultResponse {

    private String id;
    private String applicationId;
    private Integer matchPercentage;
    private List<String> matchedSkills;
    private List<String> unmatchedSkills;
    private String aiNarrativeSummary;
    private AiScreeningStageResponse resumeAnalysis;
    private AiScreeningStageResponse projectConsistency;
    private AiScreeningStageResponse inconsistencyReview;
    private String inconsistencySeverity;
    private String recommendedHumanReviewAction;
    private Instant createdAt;
    private Instant updatedAt;
}
