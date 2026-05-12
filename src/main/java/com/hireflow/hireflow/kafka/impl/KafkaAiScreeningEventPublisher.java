package com.hireflow.hireflow.kafka.impl;

import com.hireflow.hireflow.event.ApplicationSubmittedEvent;
import com.hireflow.hireflow.service.ai.AiScreeningEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaAiScreeningEventPublisher implements AiScreeningEventPublisher {

    private final KafkaTemplate<String, ApplicationSubmittedEvent> kafkaTemplate;

    @Value("${hireflow.kafka.topics.application-submitted}")
    private String applicationSubmittedTopic;

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
