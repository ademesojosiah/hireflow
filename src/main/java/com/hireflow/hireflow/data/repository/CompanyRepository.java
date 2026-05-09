package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, String> {

    Optional<Company> findByName(String name);

    boolean existsByNameIgnoreCase(String name);
}
