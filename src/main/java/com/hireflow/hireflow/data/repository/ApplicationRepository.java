package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, String> {

    boolean existsByApplicant_IdAndJobListing_Id(String applicantId, String jobListingId);

    Page<Application> findAllByApplicant_Id(String applicantId, Pageable pageable);

    Page<Application> findAllByJobListing_IdAndCompanyId(String jobListingId, String companyId, Pageable pageable);

    Optional<Application> findByIdAndApplicant_Id(String id, String applicantId);

    Optional<Application> findByIdAndCompanyId(String id, String companyId);
}
