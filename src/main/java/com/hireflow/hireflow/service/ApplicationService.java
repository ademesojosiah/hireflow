package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.ApplyToJobRequest;
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.event.events.InconsistencyReviewCompletedEvent;
import com.hireflow.hireflow.event.events.ProjectConsistencyCompletedEvent;
import com.hireflow.hireflow.event.events.ResumeAnalysisCompletedEvent;
import com.hireflow.hireflow.event.events.ScreeningCompletedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {

    ApplicationResponse applyToJob(String jobId, ApplyToJobRequest request, User user);

    Page<ApplicationResponse> findMyApplications(User user, Pageable pageable);

    ApplicationResponse findMyApplication(String applicationId, User user);

    Page<ApplicationResponse> findByJob(String jobId, User user, Pageable pageable);

    void processResumeAnalysisCompleted(ResumeAnalysisCompletedEvent event);

    void processProjectConsistencyCompleted(ProjectConsistencyCompletedEvent event);

    void processInconsistencyReviewCompleted(InconsistencyReviewCompletedEvent event);

    void processScreeningCompleted(ScreeningCompletedEvent event);
}
