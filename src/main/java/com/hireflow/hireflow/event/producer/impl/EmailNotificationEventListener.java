package com.hireflow.hireflow.event.producer.impl;

import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.event.producer.NotificationEventProducer;
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

    private final NotificationEventProducer notificationEventProducer;

    @Async("notificationAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEmailNotification(EmailNotificationEvent event) {
        switch (event.getType()) {
            case EmailNotificationEvent.OTP_VERIFICATION ->
                    notificationEventProducer.publishOtpEmail(event.getTo(), event.getOtp());
            case EmailNotificationEvent.COMPANY_WELCOME ->
                    notificationEventProducer.publishCompanyWelcomeEmail(
                            event.getTo(),
                            event.getFirstName(),
                            event.getCompanyName()
                    );
            case EmailNotificationEvent.APPLICATION_STAGE_UPDATED ->
                    notificationEventProducer.publishApplicationStageUpdate(event);
            default -> log.warn("Unknown email notification event type: {}", event.getType());
        }
    }
}
