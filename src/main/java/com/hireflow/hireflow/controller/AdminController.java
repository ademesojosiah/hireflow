package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.request.InviteHManagerRequest;
import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.dto.response.ApplicationVolumeResponse;
import com.hireflow.hireflow.dto.response.TimeToHireResponse;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.AdminMetricsService;
import com.hireflow.hireflow.service.InvitationService;
import com.hireflow.hireflow.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final InvitationService invitationService;
    private final StaffService staffService;
    private final AdminMetricsService adminMetricsService;

    @PostMapping("/invite-manager")
    public ResponseEntity<ApiResponse<Void>> inviteHManager(
            @Valid @RequestBody InviteHManagerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        invitationService.inviteHManager(request, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Invitation sent to " + request.getEmail()));
    }

    @GetMapping("/staff")
    public ResponseEntity<?> getStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Staff retrieved",
                staffService.findStaff(principal.getUser(), PageRequest.of(page, size))
        ));
    }

    @DeleteMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(
            @PathVariable String staffId,
            @AuthenticationPrincipal UserPrincipal principal) {
        staffService.deleteStaff(staffId, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Staff removed successfully"));
    }

    @GetMapping("/metrics/application-volume")
    public ResponseEntity<ApiResponse<ApplicationVolumeResponse>> getApplicationVolume(
            @RequestParam(required = false) String jobListingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Application volume retrieved",
                adminMetricsService.getApplicationVolume(principal.getUser(), jobListingId)
        ));
    }

    @GetMapping("/metrics/time-to-hire")
    public ResponseEntity<ApiResponse<TimeToHireResponse>> getTimeToHire(
            @RequestParam(required = false) String jobListingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Time to hire retrieved",
                adminMetricsService.getTimeToHire(principal.getUser(), jobListingId)
        ));
    }
}