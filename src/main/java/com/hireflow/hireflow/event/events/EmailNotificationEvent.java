package com.hireflow.hireflow.event.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationEvent {

    public static final String OTP_VERIFICATION = "OTP_VERIFICATION";
    public static final String COMPANY_WELCOME = "COMPANY_WELCOME";
    public static final String APPLICATION_STAGE_UPDATED = "APPLICATION_STAGE_UPDATED";

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

    public EmailNotificationEvent(String type, String to, String otp, String firstName, String companyName) {
        this.type = type;
        this.to = to;
        this.otp = otp;
        this.firstName = firstName;
        this.companyName = companyName;
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
}
