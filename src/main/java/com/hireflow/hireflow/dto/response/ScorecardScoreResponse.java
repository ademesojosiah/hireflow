package com.hireflow.hireflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardScoreResponse {

    private String id;
    private String criterionId;
    private String category;
    private String criterionName;
    private Integer maxScore;
    private Integer score;
    private String notes;
}
