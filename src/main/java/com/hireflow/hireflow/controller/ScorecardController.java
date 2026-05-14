package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.dto.request.SubmitScorecardRequest;
import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.ScorecardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interviews/{interviewSlotId}/scorecard")
@RequiredArgsConstructor
public class ScorecardController {

    private final ScorecardService scorecardService;

    @PostMapping
    public ResponseEntity<?> submitScorecard(
            @PathVariable String interviewSlotId,
            @RequestBody @Valid SubmitScorecardRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Scorecard submitted",
                scorecardService.submitScorecard(interviewSlotId, request, userPrincipal.getUser())
        ));
    }

    @GetMapping
    public ResponseEntity<?> getScorecard(
            @PathVariable String interviewSlotId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Scorecards retrieved",
                scorecardService.getScorecards(interviewSlotId, userPrincipal.getUser())
        ));
    }
}
