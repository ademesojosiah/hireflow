package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hireflow.hireflow.enums.ApplicationStage;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "applications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_application_applicant_job", columnNames = {"applicant_id", "job_listing_id"})
        },
        indexes = {
                @Index(name = "idx_application_company", columnList = "company_id"),
                @Index(name = "idx_application_job", columnList = "job_listing_id"),
                @Index(name = "idx_application_applicant", columnList = "applicant_id"),
                @Index(name = "idx_application_stage", columnList = "stage")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_listing_id", nullable = false)
    private JobListing jobListing;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_profile_id", nullable = false)
    private ResumeProfile resumeProfile;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStage stage = ApplicationStage.SCREENING;

    @OneToMany(
            mappedBy = "application",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<StageUpdate> stageUpdates = new ArrayList<>();

    @OneToMany(
            mappedBy = "application",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ApplicationAnswer> answers = new ArrayList<>();

    @JsonIgnore
    @OneToOne(
            mappedBy = "application",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private AiScreeningResult screeningResult;

    public void addStageUpdate(ApplicationStage previousStage, ApplicationStage currentStage, String reason, String actor) {
        StageUpdate update = new StageUpdate();
        update.setApplication(this);
        update.setPreviousStage(previousStage);
        update.setCurrentStage(currentStage);
        update.setReason(reason);
        update.setActor(actor);
        stageUpdates.add(update);
    }

    public void addAnswer(JobQuestion question, String answerText) {
        ApplicationAnswer answer = new ApplicationAnswer();
        answer.setApplication(this);
        answer.setJobQuestion(question);
        answer.setQuestionSnapshot(question.getQuestion());
        answer.setAnswer(answerText);
        answers.add(answer);
    }
}
