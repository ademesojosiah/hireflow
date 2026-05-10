package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "resume_profile_skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rps_profile_skill",
                columnNames = {"resume_profile_id", "skill_id"}
        ),
        indexes = {
                @Index(name = "idx_rps_profile", columnList = "resume_profile_id"),
                @Index(name = "idx_rps_skill", columnList = "skill_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeProfileSkill extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_profile_id", nullable = false)
    private ResumeProfile resumeProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
}
