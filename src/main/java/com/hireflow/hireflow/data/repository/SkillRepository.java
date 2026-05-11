package com.hireflow.hireflow.data.repository;

import com.hireflow.hireflow.data.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, String> {

    Optional<Skill> findByNameIgnoreCase(String name);

    List<Skill> findAllByNameIgnoreCaseIn(List<String> names);

    boolean existsByNameIgnoreCase(String name);
}
