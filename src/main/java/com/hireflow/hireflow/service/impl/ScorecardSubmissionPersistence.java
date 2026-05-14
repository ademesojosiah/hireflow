package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.InterviewSlot;
import com.hireflow.hireflow.data.model.Scorecard;
import com.hireflow.hireflow.data.model.ScorecardCriterion;
import com.hireflow.hireflow.data.model.ScorecardScore;
import com.hireflow.hireflow.data.model.ScorecardTemplate;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.InterviewSlotRepository;
import com.hireflow.hireflow.data.repository.ScorecardRepository;
import com.hireflow.hireflow.dto.request.ScorecardScoreRequest;
import com.hireflow.hireflow.dto.request.SubmitScorecardRequest;
import com.hireflow.hireflow.enums.InterviewStatus;
import com.hireflow.hireflow.enums.ScorecardStatus;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.service.ScorecardTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ScorecardSubmissionPersistence {

    private final InterviewSlotRepository interviewSlotRepository;
    private final ScorecardRepository scorecardRepository;
    private final ScorecardTemplateService scorecardTemplateService;

    @Transactional
    public Scorecard submit(String interviewSlotId, SubmitScorecardRequest request, User actor) {
        String companyId = actor.getCompany().getId();
        InterviewSlot slot = interviewSlotRepository.findByIdAndCompanyIdForUpdate(interviewSlotId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview slot not found"));

        if (slot.getStatus() == InterviewStatus.CANCELLED) {
            throw new CustomException("Cannot submit a scorecard for a cancelled interview");
        }
        if (scorecardRepository.existsByInterviewSlot_IdAndCompanyIdAndSubmittedById(
                interviewSlotId, companyId, actor.getId())) {
            throw new DuplicateResourceException("You have already submitted a scorecard for this interview");
        }

        ScorecardTemplate template = scorecardTemplateService
                .findTemplateEntityForCompany(request.getTemplateId(), companyId);
        if (!template.isActive()) {
            throw new CustomException("Cannot submit a scorecard with an inactive template");
        }
        Map<String, ScorecardCriterion> criteriaById = indexById(template.getCriteria());
        validateScoresCoverEveryCriterion(template, request.getScores(), criteriaById);

        Scorecard scorecard = new Scorecard();
        // Set only the child-side back-reference. Mutating slot.scorecards would queue an add
        // on the uninitialized lazy collection, triggering HHH90030005 on session detach.
        scorecard.setInterviewSlot(slot);
        scorecard.setApplication(slot.getApplication());
        scorecard.setCompanyId(companyId);
        scorecard.setTemplate(template);
        scorecard.setTemplateNameSnapshot(template.getName());
        scorecard.setInterviewerEmail(actor.getEmail());
        scorecard.setSubmittedById(actor.getId());
        scorecard.setSubmittedByEmail(actor.getEmail());
        scorecard.setSubmittedByRole(actor.getRole());
        scorecard.setOverallNotes(request.getOverallNotes());
        scorecard.setStatus(ScorecardStatus.SUBMITTED);
        scorecard.setSubmittedAt(Instant.now());

        int total = 0;
        int maxPossible = 0;
        for (ScorecardScoreRequest scoreRequest : request.getScores()) {
            ScorecardCriterion criterion = criteriaById.get(scoreRequest.getCriterionId());
            if (scoreRequest.getScore() > criterion.getMaxScore()) {
                throw new CustomException("Score for '" + criterion.getName()
                        + "' exceeds maxScore (" + criterion.getMaxScore() + ")");
            }
            ScorecardScore score = new ScorecardScore();
            score.setCriterion(criterion);
            score.setCriterionCategorySnapshot(criterion.getCategory());
            score.setCriterionNameSnapshot(criterion.getName());
            score.setMaxScoreSnapshot(criterion.getMaxScore());
            score.setScore(scoreRequest.getScore());
            score.setNotes(scoreRequest.getNotes());
            scorecard.addScore(score);
            total += scoreRequest.getScore();
            maxPossible += criterion.getMaxScore();
        }
        scorecard.setTotalScore(total);
        scorecard.setMaxPossibleScore(maxPossible);

        // Submitting the scorecard closes the interview.
        slot.setStatus(InterviewStatus.COMPLETED);
        interviewSlotRepository.save(slot);

        // saveAndFlush() forces the UK check inside this transaction so a concurrent
        // submit (or a double-click) that slipped past the existsBy check is reported
        // as a clean 409 DuplicateResourceException, not a 500 Internal Server Error.
        try {
            return scorecardRepository.saveAndFlush(scorecard);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("You have already submitted a scorecard for this interview");
        }
    }

    @Transactional(readOnly = true)
    public List<Scorecard> fetchAll(String interviewSlotId, User actor) {
        String companyId = actor.getCompany().getId();
        if (interviewSlotRepository.findByIdAndCompanyId(interviewSlotId, companyId).isEmpty()) {
            throw new ResourceNotFoundException("Interview slot not found");
        }
        return scorecardRepository.findAllByInterviewSlot_IdAndCompanyIdOrderByCreatedAtAsc(interviewSlotId, companyId);
    }

    private Map<String, ScorecardCriterion> indexById(List<ScorecardCriterion> criteria) {
        Map<String, ScorecardCriterion> map = new HashMap<>();
        for (ScorecardCriterion criterion : criteria) {
            map.put(criterion.getId(), criterion);
        }
        return map;
    }

    private void validateScoresCoverEveryCriterion(
            ScorecardTemplate template,
            List<ScorecardScoreRequest> scores,
            Map<String, ScorecardCriterion> criteriaById
    ) {
        if (scores.size() != template.getCriteria().size()) {
            throw new CustomException("Provide exactly one score per criterion (expected "
                    + template.getCriteria().size() + ", got " + scores.size() + ")");
        }
        Set<String> submittedCriterionIds = new HashSet<>();
        for (ScorecardScoreRequest score : scores) {
            if (!submittedCriterionIds.add(score.getCriterionId())) {
                throw new CustomException("Duplicate score for criterionId: " + score.getCriterionId());
            }
            if (!criteriaById.containsKey(score.getCriterionId())) {
                throw new ResourceNotFoundException("Unknown criterionId: " + score.getCriterionId());
            }
        }
    }
}
