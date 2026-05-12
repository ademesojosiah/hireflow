package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.event.events.ScreeningCompletedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {

    ApplicationResponse applyToJob(String jobId, User user);

    Page<ApplicationResponse> findMyApplications(User user, Pageable pageable);

    ApplicationResponse findMyApplication(String applicationId, User user);

    Page<ApplicationResponse> findByJob(String jobId, User user, Pageable pageable);

    void processScreeningCompleted(ScreeningCompletedEvent event);
}
