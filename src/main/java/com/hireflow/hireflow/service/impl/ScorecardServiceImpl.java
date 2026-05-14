package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.Scorecard;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ScorecardRepository;
import com.hireflow.hireflow.dto.request.SubmitScorecardRequest;
import com.hireflow.hireflow.dto.response.ScorecardResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.enums.ScorecardStatus;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.ScorecardMapper;
import com.hireflow.hireflow.service.ScorecardService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScorecardServiceImpl implements ScorecardService {

    private final UserService userService;
    private final ScorecardSubmissionPersistence scorecardSubmissionPersistence;
    private final ScorecardMapper scorecardMapper;
    private final ScorecardRepository scorecardRepository;

    @Override
    public ScorecardResponse submitScorecard(String interviewSlotId, SubmitScorecardRequest request, User user) {
        try {
            User actor = requireHiringManager(user);
            Scorecard scorecard = scorecardSubmissionPersistence.submit(interviewSlotId, request, actor);
            return scorecardMapper.toResponse(scorecard);
        } catch (AccessDeniedException | DuplicateResourceException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Scorecard submission failed for interview {}: {}", interviewSlotId, ex.getMessage());
            throw new CustomException("Scorecard submission failed: Internal Server Error");
        }
    }

    @Override
    public List<ScorecardResponse> getScorecards(String interviewSlotId, User user) {
        User actor = requireCompanyReviewer(user);
        return scorecardSubmissionPersistence.fetchAll(interviewSlotId, actor).stream()
                .map(scorecardMapper::toResponse)
                .toList();
    }

    @Override
    public boolean hasSubmittedScorecardForApplication(String applicationId, String companyId) {
        return scorecardRepository.existsByApplication_IdAndCompanyIdAndStatus(
                applicationId, companyId, ScorecardStatus.SUBMITTED);
    }

    private User requireHiringManager(User user) {
        User refreshed = requireCompanyReviewer(user);
        if (refreshed.getRole() != Role.HMANAGER) {
            throw new AccessDeniedException("Only hiring managers can submit scorecards");
        }
        return refreshed;
    }

    private User requireCompanyReviewer(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        User refreshed = userService.findUserById(user.getId());
        if (refreshed == null || (refreshed.getRole() != Role.ADMIN && refreshed.getRole() != Role.HMANAGER)) {
            throw new AccessDeniedException("Only admins and hiring managers can read scorecards");
        }
        if (refreshed.getCompany() == null) {
            throw new AccessDeniedException("You must belong to a company to read scorecards");
        }
        return refreshed;
    }
}
