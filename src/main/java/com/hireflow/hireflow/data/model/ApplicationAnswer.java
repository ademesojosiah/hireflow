package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "application_answers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_application_answer_question",
                        columnNames = {"application_id", "job_question_id"}
                )
        },
        indexes = {
                @Index(name = "idx_application_answer_application", columnList = "application_id"),
                @Index(name = "idx_application_answer_job_question", columnList = "job_question_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAnswer extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_question_id", nullable = false)
    private JobQuestion jobQuestion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionSnapshot;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;
}
