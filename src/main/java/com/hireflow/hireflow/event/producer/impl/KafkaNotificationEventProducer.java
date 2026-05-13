package com.hireflow.hireflow.event.producer.impl;

import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.event.producer.NotificationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationEventProducer implements NotificationEventProducer {

    private final KafkaTemplate<String, EmailNotificationEvent> kafkaTemplate;

    @Value("${hireflow.kafka.topics.notification-email}")
    private String notificationEmailTopic;

    @Override
    public void publishOtpEmail(String to, String otp) {
        EmailNotificationEvent event = new EmailNotificationEvent(
                EmailNotificationEvent.OTP_VERIFICATION,
                to,
                otp,
                null,
                null
        );
        publish(event);
    }

    @Override
    public void publishCompanyWelcomeEmail(String to, String firstName, String companyName) {
        EmailNotificationEvent event = new EmailNotificationEvent(
                EmailNotificationEvent.COMPANY_WELCOME,
                to,
                null,
                firstName,
                companyName
        );
        publish(event);
    }

    @Async("notificationAsyncExecutor")
    @Override
    public void publishApplicationStageUpdateAsync(EmailNotificationEvent event) {
        publishApplicationStageUpdate(event);
    }

    @Override
    public void publishApplicationStageUpdate(EmailNotificationEvent event) {
        publish(event);
    }

    private void publish(EmailNotificationEvent event) {
        kafkaTemplate.send(notificationEmailTopic, event.getTo(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish notification event {} to Kafka: {}", event.getType(), ex.getMessage());
                        return;
                    }

                    log.info("Published notification event {}", event.getType());
                });
    }
}
