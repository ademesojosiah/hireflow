package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.JobListingSkill;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.dto.request.JobListingRequest;
import com.hireflow.hireflow.dto.response.JobListingFilterResponse;
import com.hireflow.hireflow.dto.response.JobListingResponse;
import com.hireflow.hireflow.dto.response.SkillResponse;
import com.hireflow.hireflow.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JobListingMapper {

    private final ModelMapper modelMapper;

    public JobListing toEntity(JobListingRequest request, Company company, List<Skill> skills) {
        JobListing job = modelMapper.map(request, JobListing.class);
        job.setCompany(company);
        job.setSkills(new ArrayList<>());
        if (request.getStatus() == null) {
            job.setStatus(JobStatus.DRAFT);
        }
        attachSkills(job, skills);
        return job;
    }

    public JobListingResponse toResponse(JobListing job) {
        JobListingResponse response = modelMapper.map(job, JobListingResponse.class);
        if (job.getCompany() != null) {
            response.setCompanyId(job.getCompany().getId());
            response.setCompanyName(job.getCompany().getName());
        }
        response.setSkills(job.getSkills() == null ? List.of()
                : job.getSkills().stream()
                        .map(jls -> new SkillResponse(jls.getSkill().getId(), jls.getSkill().getName()))
                        .toList());
        return response;
    }

    public JobListingFilterResponse toJobListingFilterResponse(JobListing job) {
        JobListingFilterResponse response = modelMapper.map(job, JobListingFilterResponse.class);
        if (job.getCompany() != null) {
            response.setCompanyId(job.getCompany().getId());
            response.setCompanyName(job.getCompany().getName());
        }
        response.setSkills(job.getSkills() == null ? List.of()
                : job.getSkills().stream()
                .map(jls -> new SkillResponse(jls.getSkill().getId(), jls.getSkill().getName()))
                .toList());
        return response;
    }

    public void applyUpdate(JobListing job, JobListingRequest request, List<Skill> skills) {
        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getType() != null) job.setType(request.getType());
        if (request.getLocation() != null) job.setLocation(request.getLocation());
        if (request.getSummary() != null) job.setSummary(request.getSummary());
        if (request.getResponsibilities() != null) job.setResponsibilities(request.getResponsibilities());
        if (request.getRequiredQualifications() != null) job.setRequiredQualifications(request.getRequiredQualifications());
        if (request.getPreferredQualifications() != null) job.setPreferredQualifications(request.getPreferredQualifications());
        if (request.getStatus() != null) job.setStatus(request.getStatus());
        if (request.getAutoRejectThreshold() != null) job.setAutoRejectThreshold(request.getAutoRejectThreshold());
        if (request.getAutoPassThreshold() != null) job.setAutoPassThreshold(request.getAutoPassThreshold());
        if (skills != null) {
            job.getSkills().clear();
            attachSkills(job, skills);
        }
    }

    private void attachSkills(JobListing job, List<Skill> skills) {
        if (skills == null) return;
        for (Skill skill : skills) {
            JobListingSkill link = new JobListingSkill();
            link.setJobListing(job);
            link.setSkill(skill);
            job.getSkills().add(link);
        }
    }
}
