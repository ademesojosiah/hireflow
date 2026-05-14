package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.Scorecard;
import com.hireflow.hireflow.enums.ScorecardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScorecardRepository extends JpaRepository<Scorecard, String> {

    Optional<Scorecard> findByInterviewSlot_Id(String interviewSlotId);

    Optional<Scorecard> findByInterviewSlot_IdAndCompanyId(String interviewSlotId, String companyId);

    List<Scorecard> findAllByInterviewSlot_IdAndCompanyIdOrderByCreatedAtAsc(String interviewSlotId, String companyId);

    boolean existsByInterviewSlot_IdAndCompanyIdAndSubmittedById(
            String interviewSlotId,
            String companyId,
            String submittedById
    );

    boolean existsByApplication_IdAndCompanyIdAndStatus(String applicationId, String companyId, ScorecardStatus status);
}
