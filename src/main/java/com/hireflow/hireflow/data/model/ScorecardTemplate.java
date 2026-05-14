package com.hireflow.hireflow.data.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "scorecard_templates",
        indexes = {
                @Index(name = "idx_scorecard_template_company", columnList = "company_id"),
                @Index(name = "idx_scorecard_template_active", columnList = "is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardTemplate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(
            mappedBy = "template",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ScorecardCriterion> criteria = new ArrayList<>();

    public void addCriterion(ScorecardCriterion criterion) {
        criterion.setTemplate(this);
        criteria.add(criterion);
    }
}
