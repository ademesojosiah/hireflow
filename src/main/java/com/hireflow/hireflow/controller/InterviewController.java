package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.request.CancelInterviewRequest;
import com.hireflow.hireflow.dto.request.RescheduleInterviewRequest;
import com.hireflow.hireflow.dto.request.ScheduleInterviewRequest;
import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<?> scheduleInterview(
            @PathVariable String applicationId,
            @RequestBody @Valid ScheduleInterviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Interview scheduled",
                interviewService.scheduleInterview(applicationId, request, userPrincipal.getUser())
        ));
    }

    @PatchMapping
    public ResponseEntity<?> rescheduleInterview(
            @PathVariable String applicationId,
            @RequestBody @Valid RescheduleInterviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Interview rescheduled",
                interviewService.rescheduleInterview(applicationId, request, userPrincipal.getUser())
        ));
    }

    @DeleteMapping
    public ResponseEntity<?> cancelInterview(
            @PathVariable String applicationId,
            @RequestBody(required = false) CancelInterviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Interview cancelled",
                interviewService.cancelInterview(applicationId, request, userPrincipal.getUser())
        ));
    }

    @GetMapping
    public ResponseEntity<?> getInterview(
            @PathVariable String applicationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Interview retrieved",
                interviewService.getInterviewByApplication(applicationId, userPrincipal.getUser())
        ));
    }
}
