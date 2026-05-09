package com.hireflow.hireflow.data.dao;

import com.hireflow.hireflow.dto.response.SkillResponse;

import java.util.List;

public interface SkillSearchDao {

    List<SkillResponse> searchByNamePrefix(String prefix);
}
