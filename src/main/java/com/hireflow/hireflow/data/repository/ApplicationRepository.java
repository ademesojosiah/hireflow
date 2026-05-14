package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.enums.ScreeningRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, String> {

    boolean existsByApplicant_IdAndJobListing_Id(String applicantId, String jobListingId);

    Page<Application> findAllByApplicant_Id(String applicantId, Pageable pageable);

    Page<Application> findAllByJobListing_IdAndCompanyId(String jobListingId, String companyId, Pageable pageable);

    Page<Application> findAllByJobListing_IdAndCompanyIdAndScreeningResult_Recommendation(
            String jobListingId,
            String companyId,
            ScreeningRecommendation recommendation,
            Pageable pageable
    );

    Optional<Application> findByIdAndApplicant_Id(String id, String applicantId);

    Optional<Application> findByIdAndCompanyId(String id, String companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from Application application where application.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") String id);
}
