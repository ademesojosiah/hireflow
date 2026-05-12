package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.JobQuestion;
import com.hireflow.hireflow.data.model.JobListingSkill;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.dto.request.JobListingRequest;
import com.hireflow.hireflow.dto.request.JobQuestionRequest;
import com.hireflow.hireflow.dto.response.JobListingFilterResponse;
import com.hireflow.hireflow.dto.response.JobQuestionResponse;
import com.hireflow.hireflow.dto.response.JobListingResponse;
import com.hireflow.hireflow.dto.response.SkillResponse;
import com.hireflow.hireflow.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JobListingMapper {

    private final ModelMapper modelMapper;

    public JobListing toEntity(JobListingRequest request, Company company, List<Skill> skills) {
        JobListing job = new JobListing();
        job.setTitle(request.getTitle());
        job.setType(request.getType());
        job.setLocation(request.getLocation());
        job.setSummary(request.getSummary());
        job.setResponsibilities(request.getResponsibilities());
        job.setRequiredQualifications(request.getRequiredQualifications());
        job.setPreferredQualifications(request.getPreferredQualifications());
        job.setStatus(request.getStatus() == null ? JobStatus.DRAFT : request.getStatus());
        job.setAutoRejectThreshold(request.getAutoRejectThreshold());
        job.setAutoPassThreshold(request.getAutoPassThreshold());
        job.setCompany(company);
        job.setSkills(new ArrayList<>());
        job.setQuestions(new ArrayList<>());
        attachSkills(job, skills);
        attachQuestions(job, request.getQuestions());
        return job;
    }

    public JobListingResponse toResponse(JobListing job) {
        JobListingResponse response = new JobListingResponse();
        response.setId(job.getId());
        response.setTitle(job.getTitle());
        response.setType(job.getType());
        response.setLocation(job.getLocation());
        response.setSummary(job.getSummary());
        response.setResponsibilities(job.getResponsibilities());
        response.setRequiredQualifications(job.getRequiredQualifications());
        response.setPreferredQualifications(job.getPreferredQualifications());
        response.setStatus(job.getStatus());
        response.setAutoRejectThreshold(job.getAutoRejectThreshold());
        response.setAutoPassThreshold(job.getAutoPassThreshold());
        if (job.getCompany() != null) {
            response.setCompanyId(job.getCompany().getId());
            response.setCompanyName(job.getCompany().getName());
        }
        response.setSkills(job.getSkills() == null ? List.of()
                : job.getSkills().stream()
                  .map(jls -> new SkillResponse(jls.getSkill().getId(), jls.getSkill().getName()))
                  .toList());
        response.setQuestions(toQuestionResponses(job.getQuestions()));
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
            syncSkills(job, skills);
        }
        if (request.getQuestions() != null) {
            syncQuestions(job, request.getQuestions());
        }
    }

    private void attachSkills(JobListing job, List<Skill> skills) {
        if (skills == null) return;
        Set<String> seen = new HashSet<>();
        for (Skill skill : skills) {
            if (skill == null || skill.getId() == null) continue;
            if (!seen.add(skill.getId())) continue; // de-dupe within payload
            JobListingSkill link = new JobListingSkill();
            link.setJobListing(job);
            link.setSkill(skill);
            job.getSkills().add(link);
        }
    }


    private void syncSkills(JobListing job, List<Skill> requested) {
        Set<String> requestedIds = requested.stream()
                .filter(s -> s != null && s.getId() != null)
                .map(Skill::getId)
                .collect(Collectors.toCollection(HashSet::new));

        job.getSkills().removeIf(link -> {
            Skill s = link.getSkill();
            return s == null || s.getId() == null || !requestedIds.contains(s.getId());
        });

        Set<String> existingIds = job.getSkills().stream()
                .map(link -> link.getSkill().getId())
                .collect(Collectors.toCollection(HashSet::new));

        for (Skill skill : requested) {
            if (skill == null || skill.getId() == null) continue;
            if (!existingIds.add(skill.getId())) continue;
            JobListingSkill link = new JobListingSkill();
            link.setJobListing(job);
            link.setSkill(skill);
            job.getSkills().add(link);
        }
    }

    private void attachQuestions(JobListing job, List<JobQuestionRequest> questions) {
        if (questions == null) return;
        for (JobQuestionRequest request : questions) {
            job.getQuestions().add(toQuestion(job, request));
        }
    }

    private void syncQuestions(JobListing job, List<JobQuestionRequest> requests) {
        job.getQuestions().clear();
        attachQuestions(job, requests);
    }

    private JobQuestion toQuestion(
            JobListing job,
            JobQuestionRequest request
    ) {
        JobQuestion question = new JobQuestion();
        question.setJobListing(job);
        question.setQuestion(request.getQuestion());
        question.setAnswer(request.getAnswer());
        return question;
    }

    private List<JobQuestionResponse> toQuestionResponses(List<JobQuestion> questions) {
        if (questions == null) {
            return List.of();
        }
        return questions.stream()
                .map(question -> new JobQuestionResponse(
                        question.getId(),
                        question.getQuestion()
                ))
                .toList();
    }
}
