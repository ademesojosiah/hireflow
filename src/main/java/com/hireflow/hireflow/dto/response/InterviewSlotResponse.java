package com.hireflow.hireflow.dto.response;

import com.hireflow.hireflow.enums.InterviewStatus;
import com.hireflow.hireflow.enums.MeetingProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSlotResponse {

    private String id;
    private String applicationId;
    private Instant startTime;
    private Instant endTime;
    private String timezone;
    private MeetingProvider meetingProvider;
    private String meetingLink;
    private String interviewerEmail;
    private InterviewStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
