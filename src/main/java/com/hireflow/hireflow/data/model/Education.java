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
        name = "educations",
        indexes = @Index(name = "idx_education_profile", columnList = "resume_profile_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Education extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_profile_id", nullable = false)
    private ResumeProfile resumeProfile;

    @Column(nullable = false)
    private String institutionName;

    @Column(nullable = false)
    private String degree;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column
    private LocalDate endDate;
}
