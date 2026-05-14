package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hireflow.hireflow.enums.InterviewStatus;
import com.hireflow.hireflow.enums.MeetingProvider;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "interview_slots",
        indexes = {
                @Index(name = "idx_interview_application", columnList = "application_id"),
                @Index(name = "idx_interview_company", columnList = "company_id"),
                @Index(name = "idx_interview_status", columnList = "status"),
                @Index(name = "idx_interview_start", columnList = "start_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSlot extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    /** IANA timezone (e.g. "America/Los_Angeles"). Used by the notification template for display only. */
    @Column(nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_provider", nullable = false)
    private MeetingProvider meetingProvider = MeetingProvider.GOOGLE_MEET;

    @Column(name = "meeting_link", nullable = false, length = 500)
    private String meetingLink;

    @Column(name = "interviewer_email", nullable = false)
    private String interviewerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JsonIgnore
    @OneToMany(mappedBy = "interviewSlot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Scorecard> scorecards = new ArrayList<>();

    public void addScorecard(Scorecard scorecard) {
        scorecard.setInterviewSlot(this);
        scorecards.add(scorecard);
    }
}
