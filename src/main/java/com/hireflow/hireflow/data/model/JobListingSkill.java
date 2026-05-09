package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "job_listing_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jls_job_skill",
                columnNames = {"job_listing_id", "skill_id"}
        ),
        indexes = {
                @Index(name = "idx_jls_job", columnList = "job_listing_id"),
                @Index(name = "idx_jls_skill", columnList = "skill_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobListingSkill extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_listing_id", nullable = false)
    private JobListing jobListing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
}
