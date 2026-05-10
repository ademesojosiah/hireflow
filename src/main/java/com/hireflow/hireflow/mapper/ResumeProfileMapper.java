package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.Education;
import com.hireflow.hireflow.data.model.ResumeProfile;
import com.hireflow.hireflow.data.model.ResumeProfileSkill;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.model.WorkExperience;
import com.hireflow.hireflow.dto.request.EducationRequest;
import com.hireflow.hireflow.dto.request.ResumeProfileRequest;
import com.hireflow.hireflow.dto.request.WorkExperienceRequest;
import com.hireflow.hireflow.dto.response.EducationResponse;
import com.hireflow.hireflow.dto.response.ResumeProfileResponse;
import com.hireflow.hireflow.dto.response.SkillResponse;
import com.hireflow.hireflow.dto.response.WorkExperienceResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResumeProfileMapper {

    public ResumeProfile toEntity(ResumeProfileRequest request, User user, List<Skill> skills) {
        ResumeProfile profile = new ResumeProfile();
        profile.setUser(user);
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setLinkedIn(request.getLinkedIn());
        profile.setSummary(request.getSummary());
        profile.setSkills(new ArrayList<>());
        profile.setWorkExperiences(new ArrayList<>());
        profile.setEducations(new ArrayList<>());
        attachSkills(profile, skills);
        attachWorkExperiences(profile, request.getWorkExperiences());
        attachEducations(profile, request.getEducations());
        return profile;
    }

    public void applyUpdate(ResumeProfile profile, ResumeProfileRequest request, List<Skill> skills) {
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getLinkedIn() != null) {
            profile.setLinkedIn(request.getLinkedIn());
        }
        if (request.getSummary() != null) {
            profile.setSummary(request.getSummary());
        }

        if (skills != null) {
            profile.getSkills().clear();
            attachSkills(profile, skills);
        }

        if (request.getWorkExperiences() != null) {
            profile.getWorkExperiences().clear();
            attachWorkExperiences(profile, request.getWorkExperiences());
        }

        if (request.getEducations() != null) {
            profile.getEducations().clear();
            attachEducations(profile, request.getEducations());
        }
    }

    public ResumeProfileResponse toResponse(ResumeProfile profile) {
        ResumeProfileResponse response = new ResumeProfileResponse();
        response.setId(profile.getId());
        response.setPhoneNumber(profile.getPhoneNumber());
        response.setLinkedIn(profile.getLinkedIn());
        response.setSummary(profile.getSummary());

        User user = profile.getUser();
        if (user != null) {
            response.setUserId(user.getId());
            response.setEmail(user.getEmail());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
        }

        response.setSkills(profile.getSkills() == null ? List.of()
                : profile.getSkills().stream()
                        .map(rps -> new SkillResponse(rps.getSkill().getId(), rps.getSkill().getName()))
                        .toList());

        response.setWorkExperiences(profile.getWorkExperiences() == null ? List.of()
                : profile.getWorkExperiences().stream()
                        .map(this::toWorkExperienceResponse)
                        .toList());

        response.setEducations(profile.getEducations() == null ? List.of()
                : profile.getEducations().stream()
                        .map(this::toEducationResponse)
                        .toList());

        return response;
    }

    private void attachSkills(ResumeProfile profile, List<Skill> skills) {
        if (skills == null) return;
        for (Skill skill : skills) {
            ResumeProfileSkill resumeProfileSkill = new ResumeProfileSkill();
            resumeProfileSkill.setResumeProfile(profile);
            resumeProfileSkill.setSkill(skill);
            profile.getSkills().add(resumeProfileSkill);
        }
    }

    private void attachWorkExperiences(ResumeProfile profile, List<WorkExperienceRequest> requests) {
        if (requests == null) return;
        for (WorkExperienceRequest req : requests) {
            WorkExperience experience = new WorkExperience();
            experience.setResumeProfile(profile);
            experience.setCompanyName(req.getCompanyName());
            experience.setStartDate(req.getStartDate());
            experience.setEndDate(req.getEndDate());
            experience.setExperience(req.getExperience());
            profile.getWorkExperiences().add(experience);
        }
    }

    private void attachEducations(ResumeProfile profile, List<EducationRequest> requests) {
        if (requests == null) return;
        for (EducationRequest req : requests) {
            Education education = new Education();
            education.setResumeProfile(profile);
            education.setInstitutionName(req.getInstitutionName());
            education.setDegree(req.getDegree());
            education.setStartDate(req.getStartDate());
            education.setEndDate(req.getEndDate());
            profile.getEducations().add(education);
        }
    }

    private WorkExperienceResponse toWorkExperienceResponse(WorkExperience experience) {
        WorkExperienceResponse response = new WorkExperienceResponse();
        response.setId(experience.getId());
        response.setCompanyName(experience.getCompanyName());
        response.setStartDate(experience.getStartDate());
        response.setEndDate(experience.getEndDate());
        response.setExperience(experience.getExperience());
        return response;
    }

    private EducationResponse toEducationResponse(Education education) {
        EducationResponse response = new EducationResponse();
        response.setId(education.getId());
        response.setInstitutionName(education.getInstitutionName());
        response.setDegree(education.getDegree());
        response.setStartDate(education.getStartDate());
        response.setEndDate(education.getEndDate());
        return response;
    }
}
