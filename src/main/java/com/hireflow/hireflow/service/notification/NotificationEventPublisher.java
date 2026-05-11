package com.hireflow.hireflow.service.notification;

public interface NotificationEventPublisher {
    void publishOtpEmail(String to, String otp);

    void publishCompanyWelcomeEmail(String to, String firstName, String companyName);
}
