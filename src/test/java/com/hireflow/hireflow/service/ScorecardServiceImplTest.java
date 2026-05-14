package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.InterviewSlot;
import com.hireflow.hireflow.data.model.Scorecard;
import com.hireflow.hireflow.data.model.ScorecardCriterion;
import com.hireflow.hireflow.data.model.ScorecardTemplate;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.InterviewSlotRepository;
import com.hireflow.hireflow.data.repository.ScorecardRepository;
import com.hireflow.hireflow.dto.request.ScorecardScoreRequest;
import com.hireflow.hireflow.dto.request.SubmitScorecardRequest;
import com.hireflow.hireflow.dto.response.ScorecardResponse;
import com.hireflow.hireflow.enums.InterviewStatus;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.enums.ScorecardStatus;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.mapper.ScorecardMapper;
import com.hireflow.hireflow.service.impl.ScorecardServiceImpl;
import com.hireflow.hireflow.service.impl.ScorecardSubmissionPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScorecardServiceImplTest {

    @Mock private UserService userService;
    @Mock private ScorecardTemplateService scorecardTemplateService;
    @Mock private InterviewSlotRepository interviewSlotRepository;
    @Mock private ScorecardRepository scorecardRepository;

    private ScorecardServiceImpl scorecardService;

    private Company company;
    private User manager;
    private User admin;
    private Application application;
    private InterviewSlot interviewSlot;
    private ScorecardTemplate template;
    private ScorecardCriterion technical;
    private ScorecardCriterion behavioral;
    private ScorecardCriterion communication;
    private ScorecardCriterion cultureFit;
    private ScorecardCriterion problemSolving;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId("company-1");

        manager = new User("Maya", "Manager", "maya@example.com", "password", Role.HMANAGER, true);
        manager.setId("manager-1");
        manager.setCompany(company);

        admin = new User("Aaron", "Admin", "admin@example.com", "password", Role.ADMIN, true);
        admin.setId("admin-1");
        admin.setCompany(company);

        application = new Application();
        application.setId("application-1");
        application.setCompanyId(company.getId());

        interviewSlot = new InterviewSlot();
        interviewSlot.setId("slot-1");
        interviewSlot.setApplication(application);
        interviewSlot.setCompanyId(company.getId());
        interviewSlot.setStatus(InterviewStatus.SCHEDULED);

        template = new ScorecardTemplate();
        template.setName("Default Engineer Scorecard");
        template.setCompanyId(company.getId());

        technical = new ScorecardCriterion();
        technical.setCategory("Technical");
        technical.setName("System Design");
        technical.setMaxScore(5);
        technical.setDisplayOrder(0);
        template.addCriterion(technical);

        behavioral = new ScorecardCriterion();
        behavioral.setCategory("Behavioral");
        behavioral.setName("Communication");
        behavioral.setMaxScore(5);
        behavioral.setDisplayOrder(1);
        template.addCriterion(behavioral);

        communication = criterion("Communication", "Clarity", 2, "crit-comm");
        cultureFit = criterion("Behavioral", "Culture Fit", 3, "crit-culture");
        problemSolving = criterion("Technical", "Problem Solving", 4, "crit-problem");
        template.addCriterion(communication);
        template.addCriterion(cultureFit);
        template.addCriterion(problemSolving);

        // Give the criteria predictable IDs since they normally come from the DB.
        technical.setId("crit-tech");
        behavioral.setId("crit-behav");
        template.setId("template-1");

        ScorecardSubmissionPersistence persistence = new ScorecardSubmissionPersistence(
                interviewSlotRepository, scorecardRepository, scorecardTemplateService
        );
        scorecardService = new ScorecardServiceImpl(userService, persistence, new ScorecardMapper(), scorecardRepository);
    }

    @Test
    @DisplayName("Should submit a valid scorecard, compute totals, and mark the interview COMPLETED")
    void submitScorecard_happyPath() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(interviewSlotRepository.findByIdAndCompanyIdForUpdate("slot-1", company.getId()))
                .thenReturn(Optional.of(interviewSlot));
        when(scorecardRepository.existsByInterviewSlot_IdAndCompanyIdAndSubmittedById(
                "slot-1", company.getId(), manager.getId())).thenReturn(false);
        when(scorecardTemplateService.findTemplateEntityForCompany("template-1", company.getId()))
                .thenReturn(template);
        when(scorecardRepository.saveAndFlush(any(Scorecard.class))).thenAnswer(invocation -> {
            Scorecard saved = invocation.getArgument(0);
            saved.setId("scorecard-1");
            return saved;
        });

        SubmitScorecardRequest request = new SubmitScorecardRequest(
                "template-1",
                List.of(
                        new ScorecardScoreRequest("crit-tech", 4, "Solid design instincts"),
                        new ScorecardScoreRequest("crit-behav", 5, "Clear communicator"),
                        new ScorecardScoreRequest("crit-comm", 4, "Explains trade-offs well"),
                        new ScorecardScoreRequest("crit-culture", 4, "Collaborative"),
                        new ScorecardScoreRequest("crit-problem", 3, "Good debugging approach")
                ),
                "Strong all-round. Recommend offer."
        );

        ScorecardResponse response = scorecardService.submitScorecard("slot-1", request, manager);

        assertThat(response.getId()).isEqualTo("scorecard-1");
        assertThat(response.getStatus()).isEqualTo(ScorecardStatus.SUBMITTED);
        assertThat(response.getTotalScore()).isEqualTo(20);
        assertThat(response.getMaxPossibleScore()).isEqualTo(25);
        assertThat(response.getScores()).hasSize(5);
        assertThat(response.getInterviewerEmail()).isEqualTo("maya@example.com");
        assertThat(response.getSubmittedById()).isEqualTo("manager-1");
        assertThat(response.getSubmittedByEmail()).isEqualTo("maya@example.com");
        assertThat(response.getSubmittedByRole()).isEqualTo(Role.HMANAGER);

        // Submitting closes the interview.
        assertThat(interviewSlot.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        verify(interviewSlotRepository).save(interviewSlot);
    }

    @Test
    @DisplayName("Should reject submission when a score exceeds the criterion's maxScore")
    void submitScorecard_scoreOverMax() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(interviewSlotRepository.findByIdAndCompanyIdForUpdate("slot-1", company.getId()))
                .thenReturn(Optional.of(interviewSlot));
        when(scorecardRepository.existsByInterviewSlot_IdAndCompanyIdAndSubmittedById(
                "slot-1", company.getId(), manager.getId())).thenReturn(false);
        when(scorecardTemplateService.findTemplateEntityForCompany("template-1", company.getId()))
                .thenReturn(template);

        SubmitScorecardRequest request = new SubmitScorecardRequest(
                "template-1",
                List.of(
                        new ScorecardScoreRequest("crit-tech", 99, null),
                        new ScorecardScoreRequest("crit-behav", 3, null),
                        new ScorecardScoreRequest("crit-comm", 3, null),
                        new ScorecardScoreRequest("crit-culture", 3, null),
                        new ScorecardScoreRequest("crit-problem", 3, null)
                ),
                null
        );

        assertThatThrownBy(() -> scorecardService.submitScorecard("slot-1", request, manager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("exceeds maxScore");

        verify(scorecardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject submission when scores don't cover every template criterion")
    void submitScorecard_missingCriterion() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(interviewSlotRepository.findByIdAndCompanyIdForUpdate("slot-1", company.getId()))
                .thenReturn(Optional.of(interviewSlot));
        when(scorecardRepository.existsByInterviewSlot_IdAndCompanyIdAndSubmittedById(
                "slot-1", company.getId(), manager.getId())).thenReturn(false);
        when(scorecardTemplateService.findTemplateEntityForCompany("template-1", company.getId()))
                .thenReturn(template);

        SubmitScorecardRequest request = new SubmitScorecardRequest(
                "template-1",
                List.of(new ScorecardScoreRequest("crit-tech", 4, null)),
                null
        );

        assertThatThrownBy(() -> scorecardService.submitScorecard("slot-1", request, manager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Provide exactly one score per criterion");
    }

    @Test
    @DisplayName("Should reject a second scorecard from the same hiring manager for the same interview")
    void submitScorecard_duplicateFromSameManager() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(interviewSlotRepository.findByIdAndCompanyIdForUpdate("slot-1", company.getId()))
                .thenReturn(Optional.of(interviewSlot));
        when(scorecardRepository.existsByInterviewSlot_IdAndCompanyIdAndSubmittedById(
                "slot-1", company.getId(), manager.getId())).thenReturn(true);

        SubmitScorecardRequest request = new SubmitScorecardRequest(
                "template-1",
                List.of(new ScorecardScoreRequest("crit-tech", 4, null)),
                null
        );

        assertThatThrownBy(() -> scorecardService.submitScorecard("slot-1", request, manager))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already submitted");
    }

    @Test
    @DisplayName("Should block admins from submitting interview scorecards")
    void submitScorecard_adminForbidden() {
        when(userService.findUserById(admin.getId())).thenReturn(admin);

        SubmitScorecardRequest request = new SubmitScorecardRequest(
                "template-1",
                List.of(new ScorecardScoreRequest("crit-tech", 4, null)),
                null
        );

        assertThatThrownBy(() -> scorecardService.submitScorecard("slot-1", request, admin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only hiring managers");

        verify(scorecardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return all scorecards for an interview to company reviewers")
    void getScorecards_returnsAllInterviewScorecards() {
        Scorecard first = new Scorecard();
        first.setId("scorecard-1");
        first.setInterviewSlot(interviewSlot);
        first.setApplication(application);
        first.setCompanyId(company.getId());
        first.setTemplateNameSnapshot("Default Engineer Scorecard");
        first.setInterviewerEmail("maya@example.com");
        first.setSubmittedById("manager-1");
        first.setSubmittedByEmail("maya@example.com");
        first.setSubmittedByRole(Role.HMANAGER);
        first.setStatus(ScorecardStatus.SUBMITTED);
        first.setTotalScore(20);
        first.setMaxPossibleScore(25);

        Scorecard second = new Scorecard();
        second.setId("scorecard-2");
        second.setInterviewSlot(interviewSlot);
        second.setApplication(application);
        second.setCompanyId(company.getId());
        second.setTemplateNameSnapshot("Default Engineer Scorecard");
        second.setInterviewerEmail("nora@example.com");
        second.setSubmittedById("manager-2");
        second.setSubmittedByEmail("nora@example.com");
        second.setSubmittedByRole(Role.HMANAGER);
        second.setStatus(ScorecardStatus.SUBMITTED);
        second.setTotalScore(22);
        second.setMaxPossibleScore(25);

        when(userService.findUserById(admin.getId())).thenReturn(admin);
        when(interviewSlotRepository.findByIdAndCompanyId("slot-1", company.getId()))
                .thenReturn(Optional.of(interviewSlot));
        when(scorecardRepository.findAllByInterviewSlot_IdAndCompanyIdOrderByCreatedAtAsc("slot-1", company.getId()))
                .thenReturn(List.of(first, second));

        List<ScorecardResponse> responses = scorecardService.getScorecards("slot-1", admin);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(ScorecardResponse::getSubmittedById)
                .containsExactly("manager-1", "manager-2");
    }

    @Test
    @DisplayName("Should reject submission against a cancelled interview")
    void submitScorecard_cancelledInterview() {
        interviewSlot.setStatus(InterviewStatus.CANCELLED);
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(interviewSlotRepository.findByIdAndCompanyIdForUpdate("slot-1", company.getId()))
                .thenReturn(Optional.of(interviewSlot));

        SubmitScorecardRequest request = new SubmitScorecardRequest(
                "template-1",
                List.of(new ScorecardScoreRequest("crit-tech", 4, null)),
                null
        );

        assertThatThrownBy(() -> scorecardService.submitScorecard("slot-1", request, manager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("cancelled");
    }

    private ScorecardCriterion criterion(String category, String name, int displayOrder, String id) {
        ScorecardCriterion criterion = new ScorecardCriterion();
        criterion.setCategory(category);
        criterion.setName(name);
        criterion.setMaxScore(5);
        criterion.setDisplayOrder(displayOrder);
        criterion.setId(id);
        return criterion;
    }
}
