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
public class ScorecardTemplateResponse {

    private String id;
    private String name;
    private String description;
    private boolean active;
    private List<ScorecardCriterionResponse> criteria;
    private Instant createdAt;
    private Instant updatedAt;
}
