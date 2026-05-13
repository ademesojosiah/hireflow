package com.hireflow.hireflow.event.producer;

import com.hireflow.hireflow.event.events.EmailNotificationEvent;

public interface NotificationEventProducer {

    void publishOtpEmail(String to, String otp);

    void publishCompanyWelcomeEmail(String to, String firstName, String companyName);

    void publishApplicationStageUpdateAsync(EmailNotificationEvent event);

    void publishApplicationStageUpdate(EmailNotificationEvent event);

    void publishHManagerInviteEmail(String to, String inviteLink);
}
