package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.InterviewSlot;
import com.hireflow.hireflow.enums.InterviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, String> {

    Optional<InterviewSlot> findByApplication_IdAndStatus(String applicationId, InterviewStatus status);

    Optional<InterviewSlot> findByIdAndCompanyId(String id, String companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from InterviewSlot slot where slot.id = :id and slot.companyId = :companyId")
    Optional<InterviewSlot> findByIdAndCompanyIdForUpdate(@Param("id") String id, @Param("companyId") String companyId);

    Optional<InterviewSlot> findTopByApplication_IdAndCompanyIdOrderByCreatedAtDesc(String applicationId, String companyId);
}
