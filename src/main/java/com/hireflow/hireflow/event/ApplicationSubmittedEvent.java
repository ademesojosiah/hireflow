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
public class ApplicationSubmittedEvent {

    private String applicationId;
    private String jobListingId;
    private String jobTitle;
    private String jobSummary;
    private String requiredQualifications;
    private String preferredQualifications;
    private String applicantId;
    private String applicantEmail;
    private String resumeSummary;
    private String resumePdfUrl;
    private List<String> jobSkills;
    private List<String> applicantSkills;
    private Integer autoRejectThreshold;
    private Integer autoPassThreshold;
}
