package com.hireflow.hireflow.service;

import com.hireflow.hireflow.dto.request.SkillRequest;
import com.hireflow.hireflow.dto.response.SkillResponse;

import java.util.List;

public interface SkillService {

    SkillResponse create(SkillRequest request);

    List<SkillResponse> search(String query);
}
