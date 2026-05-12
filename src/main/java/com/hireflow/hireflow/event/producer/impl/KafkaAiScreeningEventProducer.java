package com.hireflow.hireflow.event.producer.impl;

import com.hireflow.hireflow.event.events.ApplicationSubmittedEvent;
import com.hireflow.hireflow.event.producer.AiScreeningEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaAiScreeningEventProducer implements AiScreeningEventProducer {

    private final KafkaTemplate<String, ApplicationSubmittedEvent> kafkaTemplate;

    @Value("${hireflow.kafka.topics.application-submitted}")
    private String applicationSubmittedTopic;

    @Async("notificationAsyncExecutor")
    @Override
    public void publishApplicationSubmittedAsync(ApplicationSubmittedEvent event) {
        publishApplicationSubmitted(event);
    }

    @Override
    public void publishApplicationSubmitted(ApplicationSubmittedEvent event) {
        kafkaTemplate.send(applicationSubmittedTopic, event.getApplicationId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish application submitted event {}: {}", event.getApplicationId(), ex.getMessage());
                        return;
                    }

                    log.info("Published application submitted event {}", event.getApplicationId());
                });
    }
}
