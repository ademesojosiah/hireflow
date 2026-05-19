package com.hireflow.hireflow.skill.dao;

import com.hireflow.hireflow.skill.dto.response.SkillResponse;

import java.util.List;

public interface SkillSearchDao {

    List<SkillResponse> searchByNamePrefix(String prefix);
}
