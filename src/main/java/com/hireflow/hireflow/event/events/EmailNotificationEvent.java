package com.hireflow.hireflow.event.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationEvent {

    public static final String OTP_VERIFICATION = "OTP_VERIFICATION";
    public static final String COMPANY_WELCOME = "COMPANY_WELCOME";
    public static final String APPLICATION_STAGE_UPDATED = "APPLICATION_STAGE_UPDATED";
    public static final String HMANAGER_INVITE = "HMANAGER_INVITE";

    private String type;
    private String to;
    private String otp;
    private String firstName;
    private String companyName;
    private String applicationId;
    private String applicantId;
    private String jobListingId;
    private String jobTitle;
    private String companyId;
    private String previousStage;
    private String currentStage;
    private String reason;
    private String actor;
    private String message;
    private String inviteLink;

    // Interview-specific fields (populated only when currentStage == INTERVIEW_SCHEDULED).
    // Kept optional/nullable so existing stage-change templates ignore them.
    private String meetingLink;
    private Instant interviewStartTime;
    private Instant interviewEndTime;
    private String interviewTimezone;
    private String interviewerEmail;

    public EmailNotificationEvent(String type, String to, String otp, String firstName, String companyName) {
        this.type = type;
        this.to = to;
        this.otp = otp;
        this.firstName = firstName;
        this.companyName = companyName;
    }

    public static EmailNotificationEvent hManagerInvite(String to, String inviteLink) {
        EmailNotificationEvent event = new EmailNotificationEvent();
        event.setType(HMANAGER_INVITE);
        event.setTo(to);
        event.setInviteLink(inviteLink);
        return event;
    }

    public static EmailNotificationEvent applicationStageUpdated(
            String to,
            String applicationId,
            String applicantId,
            String jobListingId,
            String jobTitle,
            String companyId,
            String companyName,
            String previousStage,
            String currentStage,
            String reason,
            String actor,
            String message
    ) {
        EmailNotificationEvent event = new EmailNotificationEvent();
        event.setType(APPLICATION_STAGE_UPDATED);
        event.setTo(to);
        event.setApplicationId(applicationId);
        event.setApplicantId(applicantId);
        event.setJobListingId(jobListingId);
        event.setJobTitle(jobTitle);
        event.setCompanyId(companyId);
        event.setCompanyName(companyName);
        event.setPreviousStage(previousStage);
        event.setCurrentStage(currentStage);
        event.setReason(reason);
        event.setActor(actor);
        event.setMessage(message);
        return event;
    }

    /**
     * Same wire type as a regular stage-change notification, but enriched with the meeting link
     * and slot details. The notification service's INTERVIEW_SCHEDULED template renders the
     * extra fields; the SSE channel just forwards the whole event.
     */
    public static EmailNotificationEvent interviewScheduled(
            String to,
            String applicationId,
            String applicantId,
            String jobListingId,
            String jobTitle,
            String companyId,
            String companyName,
            String previousStage,
            String reason,
            String actor,
            String message,
            String meetingLink,
            Instant startTime,
            Instant endTime,
            String timezone,
            String interviewerEmail
    ) {
        EmailNotificationEvent event = applicationStageUpdated(
                to, applicationId, applicantId, jobListingId, jobTitle, companyId, companyName,
                previousStage, "INTERVIEW_SCHEDULED", reason, actor, message
        );
        event.setMeetingLink(meetingLink);
        event.setInterviewStartTime(startTime);
        event.setInterviewEndTime(endTime);
        event.setInterviewTimezone(timezone);
        event.setInterviewerEmail(interviewerEmail);
        return event;
    }
}
