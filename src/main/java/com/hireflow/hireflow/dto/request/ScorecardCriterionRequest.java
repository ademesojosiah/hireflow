package com.hireflow.hireflow.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardCriterionRequest {

    @NotBlank
    @Size(max = 80)
    private String category;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer maxScore;

    @NotNull
    @Min(0)
    private Integer displayOrder;
}
