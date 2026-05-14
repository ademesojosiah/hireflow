package com.hireflow.hireflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class SubmitScorecardRequest {

    @NotBlank
    private String templateId;

    @NotEmpty(message = "Provide a score for every criterion in the template")
    @Valid
    private List<ScorecardScoreRequest> scores;

    @Size(max = 2000)
    private String overallNotes;
}
