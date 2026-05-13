package com.hireflow.hireflow.event.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
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
    private List<ApplicationSubmittedAnswer> answers = new ArrayList<>();

    public ApplicationSubmittedEvent(
            String applicationId,
            String jobListingId,
            String jobTitle,
            String jobSummary,
            String requiredQualifications,
            String preferredQualifications,
            String applicantId,
            String applicantEmail,
            String resumeSummary,
            String resumePdfUrl,
            List<String> jobSkills,
            List<String> applicantSkills,
            Integer autoRejectThreshold,
            Integer autoPassThreshold
    ) {
        this(
                applicationId,
                jobListingId,
                jobTitle,
                jobSummary,
                requiredQualifications,
                preferredQualifications,
                applicantId,
                applicantEmail,
                resumeSummary,
                resumePdfUrl,
                jobSkills,
                applicantSkills,
                autoRejectThreshold,
                autoPassThreshold,
                new ArrayList<>()
        );
    }
}
