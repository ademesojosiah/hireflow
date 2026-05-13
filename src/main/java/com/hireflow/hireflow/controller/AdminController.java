package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.request.InviteHManagerRequest;
import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final InvitationService invitationService;

    @PostMapping("/invite-manager")
    public ResponseEntity<ApiResponse<Void>> inviteHManager(
            @Valid @RequestBody InviteHManagerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        invitationService.inviteHManager(request, principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Invitation sent to " + request.getEmail()));
    }
}
