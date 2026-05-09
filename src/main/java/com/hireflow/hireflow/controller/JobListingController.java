package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.request.JobListingRequest;
import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.dto.response.JobListingResponse;
import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.JobListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobListingController {

    private final JobListingService jobListingService;

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody JobListingRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        JobListingResponse created = jobListingService.create(request, userPrincipal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job listing created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @Valid @RequestBody JobListingRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        JobListingResponse updated = jobListingService.update(id, request, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Job listing updated successfully", updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Job listing retrieved", jobListingService.findById(id)));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> findByCompany(
            @PathVariable String companyId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Job listings retrieved",
                jobListingService.findByCompany(companyId, status, PageRequest.of(page, size))));
    }

    @GetMapping
    public ResponseEntity<?> findAllOpen(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Open job listings retrieved",
                jobListingService.findAllOpen(PageRequest.of(page, size))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        jobListingService.delete(id, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Job listing deleted successfully"));
    }
}
