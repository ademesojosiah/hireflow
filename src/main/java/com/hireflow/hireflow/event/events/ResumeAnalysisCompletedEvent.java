package com.hireflow.hireflow.event.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisCompletedEvent {

    private String applicationId;
    private Integer score;
    private String explanation;
    private String review;
}
