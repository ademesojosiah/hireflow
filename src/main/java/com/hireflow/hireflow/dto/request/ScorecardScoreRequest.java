package com.hireflow.hireflow.dto.request;

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
public class ScorecardScoreRequest {

    @NotBlank
    private String criterionId;

    @NotNull
    @Min(0)
    private Integer score;

    @Size(max = 1000)
    private String notes;
}
