package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.AiScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiScreeningResultRepository extends JpaRepository<AiScreeningResult, String> {

    Optional<AiScreeningResult> findByApplication_Id(String applicationId);
}
