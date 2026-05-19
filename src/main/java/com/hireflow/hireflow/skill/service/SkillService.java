package com.hireflow.hireflow.skill.service;

import com.hireflow.hireflow.skill.dto.request.SkillRequest;
import com.hireflow.hireflow.skill.dto.response.SkillResponse;
import com.hireflow.hireflow.skill.entity.Skill;

import java.util.List;
import java.util.Set;

public interface SkillService {

    SkillResponse create(SkillRequest request);

    List<SkillResponse> search(String query);

    List<Skill> findAllByIds(Set<String> ids);

    List<Skill> findOrCreateByNames(Set<String> names);

    int seedDefaultsIfBelowMinimum(List<String> skillNames);
}
