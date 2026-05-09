package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobListingRepository extends JpaRepository<JobListing, String> {

    Page<JobListing> findAllByCompany_Id(String companyId, Pageable pageable);

    Page<JobListing> findAllByCompany_IdAndStatus(String companyId, JobStatus status, Pageable pageable);

    Page<JobListing> findAllByStatus(JobStatus status, Pageable pageable);
}
