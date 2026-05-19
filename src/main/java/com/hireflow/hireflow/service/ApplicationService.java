package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.projection.StageVolumeProjection;
import com.hireflow.hireflow.data.repository.projection.TimeToHireProjection;
import com.hireflow.hireflow.dto.request.ApplyToJobRequest;
import com.hireflow.hireflow.dto.request.BulkStageUpdateRequest;
import com.hireflow.hireflow.dto.request.StageUpdateRequest;
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.dto.response.BulkStageUpdateResponse;
import com.hireflow.hireflow.enums.ScreeningRecommendation;
import com.hireflow.hireflow.event.events.InconsistencyReviewCompletedEvent;
import com.hireflow.hireflow.event.events.ProjectConsistencyCompletedEvent;
import com.hireflow.hireflow.event.events.ResumeAnalysisCompletedEvent;
import com.hireflow.hireflow.event.events.ScreeningCompletedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse applyToJob(String jobId, ApplyToJobRequest request, User user);

    Page<ApplicationResponse> findMyApplications(User user, Pageable pageable);

    ApplicationResponse findMyApplication(String applicationId, User user);

    Page<ApplicationResponse> findByJob(String jobId, ScreeningRecommendation recommendation, User user, Pageable pageable);

    ApplicationResponse updateApplicationStage(String applicationId, StageUpdateRequest request, User user);

    BulkStageUpdateResponse bulkUpdateApplicationStage(BulkStageUpdateRequest request, User user);

    void processResumeAnalysisCompleted(ResumeAnalysisCompletedEvent event);

    void processProjectConsistencyCompleted(ProjectConsistencyCompletedEvent event);

    void processInconsistencyReviewCompleted(InconsistencyReviewCompletedEvent event);

    void processScreeningCompleted(ScreeningCompletedEvent event);


    List<StageVolumeProjection> countApplicationsByStage(String companyId, String jobListingId);


    List<TimeToHireProjection> findHireDurations(String companyId, String jobListingId);
}
