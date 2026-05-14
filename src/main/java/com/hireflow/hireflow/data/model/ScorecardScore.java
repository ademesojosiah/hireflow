package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "scorecard_scores",
        indexes = @Index(name = "idx_scorecard_score_scorecard", columnList = "scorecard_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardScore extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scorecard_id", nullable = false)
    private Scorecard scorecard;

    /** Live reference; nullable so deleted criteria don't break historical scorecards. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id")
    private ScorecardCriterion criterion;

    /** Snapshot fields — survive template edits/deletes so the scorecard reads correctly forever. */
    @Column(name = "criterion_category_snapshot", nullable = false)
    private String criterionCategorySnapshot;

    @Column(name = "criterion_name_snapshot", nullable = false)
    private String criterionNameSnapshot;

    @Column(name = "max_score_snapshot", nullable = false)
    private Integer maxScoreSnapshot;

    @Column(nullable = false)
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
