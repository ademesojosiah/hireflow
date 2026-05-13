package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hireflow.hireflow.enums.ScreeningRecommendation;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ai_screening_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_screening_application", columnNames = "application_id"),
        indexes = {
                @Index(name = "idx_ai_screening_application", columnList = "application_id"),
                @Index(name = "idx_ai_screening_recommendation", columnList = "recommendation")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiScreeningResult extends BaseEntity {

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Min(0)
    @Max(100)
    @Column
    private Integer matchPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false)
    private ScreeningRecommendation recommendation = ScreeningRecommendation.PENDING;

    @ElementCollection
    @CollectionTable(name = "ai_screening_matched_skills", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "skill")
    private List<String> matchedSkills = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "ai_screening_unmatched_skills", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "skill")
    private List<String> unmatchedSkills = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String aiNarrativeSummary;

    @Min(0)
    @Max(100)
    @Column
    private Integer resumeAnalysisScore;

    @Column(columnDefinition = "TEXT")
    private String resumeAnalysisExplanation;

    @Column(columnDefinition = "TEXT")
    private String resumeAnalysisReview;

    @Min(0)
    @Max(100)
    @Column
    private Integer projectConsistencyScore;

    @Column(columnDefinition = "TEXT")
    private String projectConsistencyExplanation;

    @Column(columnDefinition = "TEXT")
    private String projectConsistencyReview;

    @Min(0)
    @Max(100)
    @Column
    private Integer inconsistencyScore;

    @Column
    private String inconsistencySeverity;

    @Column(columnDefinition = "TEXT")
    private String inconsistencyExplanation;

    @Column(columnDefinition = "TEXT")
    private String inconsistencyReview;

    @Column(columnDefinition = "TEXT")
    private String recommendedHumanReviewAction;

}
