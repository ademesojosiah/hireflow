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
}
