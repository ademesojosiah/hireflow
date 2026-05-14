package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.enums.ScorecardStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "scorecards",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_scorecard_interview_submitter",
                columnNames = {"interview_slot_id", "submitted_by_id"}
        ),
        indexes = {
                @Index(name = "idx_scorecard_application", columnList = "application_id"),
                @Index(name = "idx_scorecard_company", columnList = "company_id"),
                @Index(name = "idx_scorecard_interview_slot", columnList = "interview_slot_id"),
                @Index(name = "idx_scorecard_submitter", columnList = "submitted_by_id"),
                @Index(name = "idx_scorecard_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Scorecard extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_slot_id", nullable = false)
    private InterviewSlot interviewSlot;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    /** Template the interviewer chose at submission time. May be null if template was deleted afterwards. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ScorecardTemplate template;

    /** Snapshot of the template name at submission — survives template rename or deletion. */
    @Column(name = "template_name_snapshot", nullable = false)
    private String templateNameSnapshot;

    @Column(name = "interviewer_email", nullable = false)
    private String interviewerEmail;

    @Column(name = "submitted_by_id", nullable = false)
    private String submittedById;

    @Column(name = "submitted_by_email", nullable = false)
    private String submittedByEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "submitted_by_role", nullable = false)
    private Role submittedByRole;

    @Column(name = "overall_notes", columnDefinition = "TEXT")
    private String overallNotes;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(name = "max_possible_score", nullable = false)
    private Integer maxPossibleScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScorecardStatus status = ScorecardStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @OneToMany(
            mappedBy = "scorecard",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ScorecardScore> scores = new ArrayList<>();

    public void addScore(ScorecardScore score) {
        score.setScorecard(this);
        scores.add(score);
    }
}
