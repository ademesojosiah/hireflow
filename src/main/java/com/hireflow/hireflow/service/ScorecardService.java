package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.SubmitScorecardRequest;
import com.hireflow.hireflow.dto.response.ScorecardResponse;

import java.util.List;

public interface ScorecardService {

    /** Records the scorecard for an interview and marks the interview COMPLETED. */
    ScorecardResponse submitScorecard(String interviewSlotId, SubmitScorecardRequest request, User user);

    List<ScorecardResponse> getScorecards(String interviewSlotId, User user);

    boolean hasSubmittedScorecardForApplication(String applicationId, String companyId);
}
