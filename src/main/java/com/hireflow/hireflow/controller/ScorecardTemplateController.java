package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.request.ScorecardTemplateRequest;
import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.ScorecardTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/scorecard-templates")
@RequiredArgsConstructor
public class ScorecardTemplateController {

    private final ScorecardTemplateService scorecardTemplateService;

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody @Valid ScorecardTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Scorecard template created",
                scorecardTemplateService.createTemplate(request, userPrincipal.getUser())
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody @Valid ScorecardTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Scorecard template updated",
                scorecardTemplateService.updateTemplate(id, request, userPrincipal.getUser())
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Scorecard template retrieved",
                scorecardTemplateService.findTemplate(id, userPrincipal.getUser())
        ));
    }

    @GetMapping
    public ResponseEntity<?> findAll(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Scorecard templates retrieved",
                scorecardTemplateService.findTemplates(activeOnly, userPrincipal.getUser(), PageRequest.of(page, size))
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivate(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        scorecardTemplateService.deactivateTemplate(id, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Scorecard template deactivated", null));
    }
}
