package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.ResumeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeProfileRepository extends JpaRepository<ResumeProfile, String> {

    Optional<ResumeProfile> findByUser_Id(String userId);

    boolean existsByUser_Id(String userId);
}
