package com.hireflow.hireflow.service.notification;

import com.hireflow.hireflow.event.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationEventListener {

    private final NotificationEventPublisher notificationEventPublisher;

    @Async("notificationAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEmailNotification(EmailNotificationEvent event) {
        switch (event.getType()) {
            case EmailNotificationEvent.OTP_VERIFICATION ->
                    notificationEventPublisher.publishOtpEmail(event.getTo(), event.getOtp());
            case EmailNotificationEvent.COMPANY_WELCOME ->
                    notificationEventPublisher.publishCompanyWelcomeEmail(
                            event.getTo(),
                            event.getFirstName(),
                            event.getCompanyName()
                    );
            default -> log.warn("Unknown email notification event type: {}", event.getType());
        }
    }
}
