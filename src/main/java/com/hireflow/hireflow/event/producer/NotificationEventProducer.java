package com.hireflow.hireflow.event.producer;

public interface NotificationEventProducer {

    void publishOtpEmail(String to, String otp);

    void publishCompanyWelcomeEmail(String to, String firstName, String companyName);
}
