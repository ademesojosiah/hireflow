package com.hireflow.hireflow.service.notification;

import com.hireflow.hireflow.event.EmailNotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailNotificationEventListenerTest {

    @Test
    @DisplayName("Should publish OTP email when OTP event is handled")
    void handleOtpEmailNotification() {
        NotificationEventPublisher notificationEventPublisher = mock(NotificationEventPublisher.class);
        EmailNotificationEventListener listener = new EmailNotificationEventListener(notificationEventPublisher);

        listener.handleEmailNotification(new EmailNotificationEvent(
                EmailNotificationEvent.OTP_VERIFICATION,
                "user@example.com",
                "123456",
                null,
                null
        ));

        verify(notificationEventPublisher).publishOtpEmail("user@example.com", "123456");
    }

    @Test
    @DisplayName("Should publish company welcome email when company welcome event is handled")
    void handleCompanyWelcomeEmailNotification() {
        NotificationEventPublisher notificationEventPublisher = mock(NotificationEventPublisher.class);
        EmailNotificationEventListener listener = new EmailNotificationEventListener(notificationEventPublisher);

        listener.handleEmailNotification(new EmailNotificationEvent(
                EmailNotificationEvent.COMPANY_WELCOME,
                "admin@example.com",
                null,
                "Alice",
                "Acme"
        ));

        verify(notificationEventPublisher).publishCompanyWelcomeEmail("admin@example.com", "Alice", "Acme");
    }
}
