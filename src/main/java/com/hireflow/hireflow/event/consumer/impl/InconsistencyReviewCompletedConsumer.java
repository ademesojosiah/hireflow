package com.hireflow.hireflow.event.consumer.impl;

import com.hireflow.hireflow.event.events.InconsistencyReviewCompletedEvent;
import com.hireflow.hireflow.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InconsistencyReviewCompletedConsumer {

    private final ApplicationService applicationService;

    @KafkaListener(
            topics = "${hireflow.kafka.topics.inconsistency-review-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type=com.hireflow.hireflow.event.events.InconsistencyReviewCompletedEvent"
            }
    )
    public void consume(InconsistencyReviewCompletedEvent event) {
        log.info("Received inconsistency review completed event for application {}", event.getApplicationId());
        applicationService.processInconsistencyReviewCompleted(event);
    }
}
