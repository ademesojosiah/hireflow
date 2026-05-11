package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "resume_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_resume_profile_user", columnNames = "user_id"),
        indexes = @Index(name = "idx_resume_profile_user", columnList = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeProfile extends BaseEntity {

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private String phoneNumber;

    @Column
    private String linkedIn;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 2000)
    private String pdfUrl;

    @OneToMany(
            mappedBy = "resumeProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ResumeProfileSkill> skills = new ArrayList<>();

    @OneToMany(
            mappedBy = "resumeProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<WorkExperience> workExperiences = new ArrayList<>();

    @OneToMany(
            mappedBy = "resumeProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Education> educations = new ArrayList<>();
}
