package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.JobListingRequest;
import com.hireflow.hireflow.dto.response.JobListingResponse;
import com.hireflow.hireflow.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobListingService {

    JobListingResponse create(JobListingRequest request, User user);

    JobListingResponse update(String id, JobListingRequest request, User user);

    JobListingResponse findById(String id);

    Page<JobListingResponse> findByCompany(String companyId, JobStatus status, Pageable pageable);

    Page<JobListingResponse> findAllOpen(Pageable pageable);

    void delete(String id, User user);
}
