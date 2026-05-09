package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.dto.request.SkillRequest;
import com.hireflow.hireflow.dto.response.SkillResponse;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {

    public Skill toEntity(SkillRequest request) {
        Skill skill = new Skill();
        skill.setName(request.getName());
        return skill;
    }

    public SkillResponse toResponse(Skill skill) {
        SkillResponse response = new SkillResponse();
        response.setId(skill.getId());
        response.setName(skill.getName());
        return response;
    }
}
