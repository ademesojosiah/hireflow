package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/jobs/{jobId}")
    public ResponseEntity<?> applyToJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ApplicationResponse response = applicationService.applyToJob(jobId, userPrincipal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted successfully", response));
    }

    @GetMapping("")
    public ResponseEntity<?> findMyApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Applications retrieved",
                applicationService.findMyApplications(userPrincipal.getUser(), PageRequest.of(page, size))
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findMyApplication(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Application retrieved",
                applicationService.findMyApplication(id, userPrincipal.getUser())
        ));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> findByJob(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Job applications retrieved",
                applicationService.findByJob(jobId, userPrincipal.getUser(), PageRequest.of(page, size))
        ));
    }
}
