package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.ScorecardTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScorecardTemplateRepository extends JpaRepository<ScorecardTemplate, String> {

    Page<ScorecardTemplate> findAllByCompanyIdAndActive(String companyId, boolean active, Pageable pageable);

    Page<ScorecardTemplate> findAllByCompanyId(String companyId, Pageable pageable);

    Optional<ScorecardTemplate> findByIdAndCompanyId(String id, String companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name);
}
