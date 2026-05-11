package com.hireflow.hireflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeProfileRequest {

    @Size(max = 30)
    private String phoneNumber;

    @Size(max = 300)
    private String linkedIn;

    @Size(max = 5000)
    private String summary;

    @Size(max = 2000)
    private String resumePdfUrl;

    @Size(max = 500)
    private String resumePublicId;

    private Set<String> skillNames;

    @Valid
    private List<WorkExperienceRequest> workExperiences;

    @Valid
    private List<EducationRequest> educations;
}
