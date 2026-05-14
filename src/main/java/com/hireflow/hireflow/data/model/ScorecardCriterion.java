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
        name = "scorecard_criteria",
        indexes = @Index(name = "idx_scorecard_criterion_template", columnList = "template_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardCriterion extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ScorecardTemplate template;

    /** Free-form bucket, e.g. "Technical", "Behavioral", "Communication". */
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Inclusive upper bound of the score. Typically 5. */
    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
