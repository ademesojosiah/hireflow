package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.repository.projection.StageVolumeProjection;
import com.hireflow.hireflow.data.repository.projection.TimeToHireProjection;
import com.hireflow.hireflow.enums.ScreeningRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
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
    @Query("select application from Application application where application.id = :id and application.companyId = :companyId")
    Optional<Application> findByIdAndCompanyIdForUpdate(@Param("id") String id, @Param("companyId") String companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from Application application where application.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") String id);

    @Query("""
            SELECT a.stage AS stage, COUNT(a) AS count
            FROM Application a
            WHERE a.companyId = :companyId
              AND (:jobListingId IS NULL OR a.jobListing.id = :jobListingId)
            GROUP BY a.stage
            """)
    List<StageVolumeProjection> countByStageForCompany(
            @Param("companyId") String companyId,
            @Param("jobListingId") String jobListingId
    );

    @Query("""
            SELECT a.createdAt AS appliedAt, MIN(su.createdAt) AS hiredAt
            FROM Application a
            JOIN a.stageUpdates su
            WHERE a.companyId = :companyId
              AND a.stage = com.hireflow.hireflow.enums.ApplicationStage.HIRED
              AND su.currentStage = com.hireflow.hireflow.enums.ApplicationStage.HIRED
              AND (:jobListingId IS NULL OR a.jobListing.id = :jobListingId)
            GROUP BY a.id, a.createdAt
            """)
    List<TimeToHireProjection> findHiredDurationsForCompany(
            @Param("companyId") String companyId,
            @Param("jobListingId") String jobListingId
    );
}
