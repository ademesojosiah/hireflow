package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "work_experiences",
        indexes = @Index(name = "idx_work_experience_profile", columnList = "resume_profile_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkExperience extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_profile_id", nullable = false)
    private ResumeProfile resumeProfile;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String experience;
}
