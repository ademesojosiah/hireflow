package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobListingRepository extends JpaRepository<JobListing, String> {

    Page<JobListing> findAllByCompany_Id(String companyId, Pageable pageable);

    Page<JobListing> findAllByCompany_IdAndStatus(String companyId, JobStatus status, Pageable pageable);

    Page<JobListing> findAllByStatus(JobStatus status, Pageable pageable);

    @Query("""
            SELECT job
            FROM JobListing job
            WHERE job.status = com.hireflow.hireflow.enums.JobStatus.OPEN
            AND (:title IS NULL OR LOWER(job.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:type IS NULL OR job.type = :type)
            """)
    Page<JobListing> findAllOpen(
            @Param("title") String title,
            @Param("type") JobType type,
            Pageable pageable);
}
