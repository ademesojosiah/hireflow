package com.hireflow.hireflow.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningCompletedEvent {

    private String applicationId;
    private Integer matchPercentage;
    private List<String> matchedSkills;
    private List<String> unmatchedSkills;
    private String aiNarrativeSummary;
}
