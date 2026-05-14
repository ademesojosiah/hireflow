package com.hireflow.hireflow.dto.response;

import com.hireflow.hireflow.enums.ScorecardStatus;
import com.hireflow.hireflow.enums.Role;
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
public class ScorecardResponse {

    private String id;
    private String interviewSlotId;
    private String applicationId;
    private String templateId;
    private String templateName;
    private String interviewerEmail;
    private String submittedById;
    private String submittedByEmail;
    private Role submittedByRole;
    private String overallNotes;
    private Integer totalScore;
    private Integer maxPossibleScore;
    private ScorecardStatus status;
    private Instant submittedAt;
    private List<ScorecardScoreResponse> scores;
    private Instant createdAt;
    private Instant updatedAt;
}
