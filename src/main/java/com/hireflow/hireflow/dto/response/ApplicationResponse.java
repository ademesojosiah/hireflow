package com.hireflow.hireflow.dto.response;

import com.hireflow.hireflow.enums.ApplicationStage;
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
public class ApplicationResponse {

    private String id;
    private ApplicationStage stage;
    private String applicantId;
    private String applicantName;
    private String applicantEmail;
    private String jobListingId;
    private String jobTitle;
    private String companyId;
    private String companyName;
    private ResumeProfileResponse resumeProfile;
    private AiScreeningResultResponse screeningResult;
    private List<StageUpdateResponse> stageUpdates;
    private List<ApplicationAnswerResponse> answers;
    private Instant createdAt;
    private Instant updatedAt;
}
