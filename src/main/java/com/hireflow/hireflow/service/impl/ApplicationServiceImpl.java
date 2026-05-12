package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.AiScreeningResult;
import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.AiScreeningResultRepository;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.event.events.ScreeningCompletedEvent;
import com.hireflow.hireflow.event.producer.AiScreeningEventProducer;
import com.hireflow.hireflow.mapper.ApplicationMapper;
import com.hireflow.hireflow.service.ApplicationService;
import com.hireflow.hireflow.service.JobListingService;
import com.hireflow.hireflow.service.UserService;
import com.hireflow.hireflow.service.result.ApplicationSubmissionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final AiScreeningResultRepository aiScreeningResultRepository;
    private final UserService userService;
    private final JobListingService jobListingService;
    private final AiScreeningEventProducer aiScreeningEventProducer;
    private final ApplicationMapper applicationMapper;
    private final ApplicationSubmissionPersistence applicationSubmissionPersistence;

    @Override
    public ApplicationResponse applyToJob(String jobId, User user) {
        try {
            ApplicationSubmissionResult result = applicationSubmissionPersistence.submit(jobId, user);
            aiScreeningEventProducer.publishApplicationSubmittedAsync(result.getEvent());
            return result.getResponse();
        } catch (AccessDeniedException | DuplicateResourceException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Application submission failed: {}", ex.getMessage());
            throw new CustomException("Application submission failed: Internal Server Error");
        }
    }

    @Override
    public Page<ApplicationResponse> findMyApplications(User user, Pageable pageable) {
        User applicant = requireApplicant(user);
        return applicationRepository.findAllByApplicant_Id(applicant.getId(), pageable)
                .map(applicationMapper::toResponse);
    }

    @Override
    public ApplicationResponse findMyApplication(String applicationId, User user) {
        User applicant = requireApplicant(user);
        Application application = applicationRepository.findByIdAndApplicant_Id(applicationId, applicant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return applicationMapper.toResponse(application);
    }

    @Override
    public Page<ApplicationResponse> findByJob(String jobId, User user, Pageable pageable) {
        User manager = requireCompanyManager(user);
        JobListing job = jobListingService.findJobListingById(jobId);
        if (!manager.getCompany().getId().equals(job.getCompany().getId())) {
            throw new AccessDeniedException("You can only view applications for your company");
        }

        return applicationRepository.findAllByJobListing_IdAndCompanyId(jobId, manager.getCompany().getId(), pageable)
                .map(applicationMapper::toResponse);
    }

    @Override
    @Transactional
    public void processScreeningCompleted(ScreeningCompletedEvent event) {
        try {
            Application application = applicationRepository.findById(event.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

            AiScreeningResult result = aiScreeningResultRepository.findByApplication_Id(application.getId())
                    .orElseGet(AiScreeningResult::new);
            result.setApplication(application);
            result.setMatchPercentage(event.getMatchPercentage());
            result.setMatchedSkills(safeList(event.getMatchedSkills()));
            result.setUnmatchedSkills(safeList(event.getUnmatchedSkills()));
            result.setAiNarrativeSummary(event.getAiNarrativeSummary());
            application.setScreeningResult(result);

            ApplicationStage nextStage = resolveScreeningStage(application, event.getMatchPercentage());
            if (nextStage != application.getStage()) {
                ApplicationStage previous = application.getStage();
                application.setStage(nextStage);
                application.addStageUpdate(previous, nextStage, "AI screening completed", "ai-screening");
            }

            applicationRepository.save(application);
            log.info("Processed AI screening result for application {}", application.getId());
        } catch (ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process AI screening result: {}", ex.getMessage());
            throw new CustomException("Failed to process AI screening result");
        }
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

    private User requireCompanyManager(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }

        User refreshed = userService.findUserById(user.getId());
        if (refreshed == null || (refreshed.getRole() != Role.ADMIN && refreshed.getRole() != Role.HMANAGER)) {
            throw new AccessDeniedException("Only admins and hiring managers can view job applications");
        }
        if (refreshed.getCompany() == null) {
            throw new AccessDeniedException("You must belong to a company to view job applications");
        }
        return refreshed;
    }

    private ApplicationStage resolveScreeningStage(Application application, Integer matchPercentage) {
        int score = matchPercentage == null ? 0 : matchPercentage;
        JobListing job = application.getJobListing();

        if (score < job.getAutoRejectThreshold()) {
            return ApplicationStage.REJECTED;
        }
        if (score >= job.getAutoPassThreshold()) {
            return ApplicationStage.INTERVIEW_SCHEDULED;
        }
        return ApplicationStage.SCREENING;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
