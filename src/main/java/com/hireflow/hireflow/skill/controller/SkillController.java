package com.hireflow.hireflow.skill.controller;

import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.skill.dto.request.SkillRequest;
import com.hireflow.hireflow.skill.dto.response.SkillResponse;
import com.hireflow.hireflow.skill.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody SkillRequest request) {
        SkillResponse created = skillService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Skill created successfully", created));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query) {
        List<SkillResponse> skills = skillService.search(query);
        return ResponseEntity.ok(ApiResponse.success("Skills retrieved", skills));
    }
}
