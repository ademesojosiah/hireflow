package com.hireflow.hireflow.event.producer.impl;

import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.event.producer.NotificationEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailNotificationEventListenerTest {

    @Test
    @DisplayName("Should publish OTP email when OTP event is handled")
    void handleOtpEmailNotification() {
        NotificationEventProducer notificationEventProducer = mock(NotificationEventProducer.class);
        EmailNotificationEventListener listener = new EmailNotificationEventListener(notificationEventProducer);

        listener.handleEmailNotification(new EmailNotificationEvent(
                EmailNotificationEvent.OTP_VERIFICATION,
                "user@example.com",
                "123456",
                null,
                null
        ));

        verify(notificationEventProducer).publishOtpEmail("user@example.com", "123456");
    }

    @Test
    @DisplayName("Should publish company welcome email when company welcome event is handled")
    void handleCompanyWelcomeEmailNotification() {
        NotificationEventProducer notificationEventProducer = mock(NotificationEventProducer.class);
        EmailNotificationEventListener listener = new EmailNotificationEventListener(notificationEventProducer);

        listener.handleEmailNotification(new EmailNotificationEvent(
                EmailNotificationEvent.COMPANY_WELCOME,
                "admin@example.com",
                null,
                "Alice",
                "Acme"
        ));

        verify(notificationEventProducer).publishCompanyWelcomeEmail("admin@example.com", "Alice", "Acme");
    }
}
