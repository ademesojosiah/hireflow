package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.request.ResumeProfileRequest;
import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.dto.response.ResumeProfileResponse;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.ResumeProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/resume-profiles")
@RequiredArgsConstructor
public class ResumeProfileController {

    private final ResumeProfileService resumeProfileService;

    @PutMapping
    public ResponseEntity<?> upsertMyProfile(
            @Valid @RequestBody ResumeProfileRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ResumeProfileResponse saved = resumeProfileService.upsertMyProfile(request, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Resume profile saved", saved));
    }

    @GetMapping
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        ResumeProfileResponse profile = resumeProfileService.getMyProfile(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Resume profile retrieved", profile));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> findByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("Resume profile retrieved",
                resumeProfileService.findByUserId(userId)));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteMyProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        resumeProfileService.deleteMyProfile(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Resume profile deleted"));
    }

}
