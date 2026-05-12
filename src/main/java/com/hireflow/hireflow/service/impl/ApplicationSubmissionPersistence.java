package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.ResumeProfile;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.event.events.ApplicationSubmittedEvent;
import com.hireflow.hireflow.mapper.ApplicationMapper;
import com.hireflow.hireflow.service.JobListingService;
import com.hireflow.hireflow.service.ResumeProfileService;
import com.hireflow.hireflow.service.UserService;
import com.hireflow.hireflow.service.result.ApplicationSubmissionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ApplicationSubmissionPersistence {

    private final ApplicationRepository applicationRepository;
    private final UserService userService;
    private final JobListingService jobListingService;
    private final ResumeProfileService resumeProfileService;
    private final ApplicationMapper applicationMapper;

    @Transactional
    public ApplicationSubmissionResult submit(String jobId, User user) {
        User applicant = requireApplicant(user);
        JobListing job = jobListingService.findJobListingById(jobId);
        validateOpenJob(job);
        ResumeProfile resumeProfile = resumeProfileService.findProfileByUserId(applicant.getId());

        if (applicationRepository.existsByApplicant_IdAndJobListing_Id(applicant.getId(), job.getId())) {
            throw new DuplicateResourceException("You have already applied to this job");
        }

        Application application = new Application();
        application.setApplicant(applicant);
        application.setJobListing(job);
        application.setResumeProfile(resumeProfile);
        application.setCompanyId(job.getCompany().getId());
        application.setStage(ApplicationStage.SCREENING);
        application.addStageUpdate(null, ApplicationStage.APPLIED, "Application submitted", applicant.getEmail());
        application.addStageUpdate(ApplicationStage.APPLIED, ApplicationStage.SCREENING, "Queued for AI screening", "system");

        Application saved = applicationRepository.save(application);
        ApplicationResponse response = applicationMapper.toResponse(saved);
        ApplicationSubmittedEvent event = toSubmittedEvent(saved);
        return new ApplicationSubmissionResult(response, event);
    }

    private User requireApplicant(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }

        User refreshed = userService.findUserById(user.getId());
        if (refreshed == null || refreshed.getRole() != Role.APPLICANT) {
            throw new AccessDeniedException("Only applicants can apply to jobs");
        }
        return refreshed;
    }

    private void validateOpenJob(JobListing job) {
        if (job.getStatus() != JobStatus.OPEN) {
            throw new CustomException("This job is not accepting applications");
        }
    }

    private ApplicationSubmittedEvent toSubmittedEvent(Application application) {
        JobListing job = application.getJobListing();
        ResumeProfile resumeProfile = application.getResumeProfile();
        User applicant = application.getApplicant();

        return new ApplicationSubmittedEvent(
                application.getId(),
                job.getId(),
                job.getTitle(),
                job.getSummary(),
                job.getRequiredQualifications(),
                job.getPreferredQualifications(),
                applicant.getId(),
                applicant.getEmail(),
                resumeProfile.getSummary(),
                resumeProfile.getResumePdfUrl(),
                job.getSkills().stream().map(link -> link.getSkill().getName()).toList(),
                resumeProfile.getSkills().stream().map(link -> link.getSkill().getName()).toList(),
                job.getAutoRejectThreshold(),
                job.getAutoPassThreshold()
        );
    }
}
