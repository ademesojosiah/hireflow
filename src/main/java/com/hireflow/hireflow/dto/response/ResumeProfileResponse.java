package com.hireflow.hireflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeProfileResponse {

    private String id;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String linkedIn;
    private String summary;
    private String pdfUrl;
    private List<SkillResponse> skills;
    private List<WorkExperienceResponse> workExperiences;
    private List<EducationResponse> educations;
}
