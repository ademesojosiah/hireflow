package com.hireflow.hireflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardTemplateRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "criteria is required")
    @Size(min = 5, max = 5, message = "A scorecard template must define exactly 5 criteria")
    @Valid
    private List<ScorecardCriterionRequest> criteria;
}
