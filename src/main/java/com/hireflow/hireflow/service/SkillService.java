package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.dto.request.SkillRequest;
import com.hireflow.hireflow.dto.response.SkillResponse;

import java.util.List;
import java.util.Set;

public interface SkillService {

    SkillResponse create(SkillRequest request);

    List<SkillResponse> search(String query);

    List<Skill> findAllByIds(Set<String> ids);

    int seedDefaultsIfBelowMinimum(List<String> skillNames);
}
