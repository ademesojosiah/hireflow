package com.hireflow.hireflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardCriterionResponse {

    private String id;
    private String category;
    private String name;
    private String description;
    private Integer maxScore;
    private Integer displayOrder;
}
