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
    private Integer matchPercentage;
    private List<String> matchedSkills;
    private List<String> unmatchedSkills;
    private Instant createdAt;
}
