package com.hireflow.hireflow.skill.mapper;

import com.hireflow.hireflow.skill.dto.request.SkillRequest;
import com.hireflow.hireflow.skill.dto.response.SkillResponse;
import com.hireflow.hireflow.skill.entity.Skill;
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
