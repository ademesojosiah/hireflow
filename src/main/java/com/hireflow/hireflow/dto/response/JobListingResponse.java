package com.hireflow.hireflow.dto.response;

import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobListingResponse {

    private String id;
    private String title;
    private JobType type;
    private String location;
    private String summary;
    private String responsibilities;
    private String requiredQualifications;
    private String preferredQualifications;
    private JobStatus status;
    private Integer autoRejectThreshold;
    private Integer autoPassThreshold;
    private String companyId;
    private String companyName;
    private List<SkillResponse> skills;
    private List<JobQuestionResponse> questions;

    public JobListingResponse(
            String id,
            String title,
            JobType type,
            String location,
            String summary,
            String responsibilities,
            String requiredQualifications,
            String preferredQualifications,
            JobStatus status,
            Integer autoRejectThreshold,
            Integer autoPassThreshold,
            String companyId,
            String companyName,
            List<SkillResponse> skills
    ) {
        this(
                id,
                title,
                type,
                location,
                summary,
                responsibilities,
                requiredQualifications,
                preferredQualifications,
                status,
                autoRejectThreshold,
                autoPassThreshold,
                companyId,
                companyName,
                skills,
                List.of()
        );
    }
}
