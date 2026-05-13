package com.hireflow.hireflow.event.consumer.impl;

import com.hireflow.hireflow.event.events.ProjectConsistencyCompletedEvent;
import com.hireflow.hireflow.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectConsistencyCompletedConsumer {

    private final ApplicationService applicationService;

    @KafkaListener(
            topics = "${hireflow.kafka.topics.project-consistency-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type=com.hireflow.hireflow.event.events.ProjectConsistencyCompletedEvent"
            }
    )
    public void consume(ProjectConsistencyCompletedEvent event) {
        log.info("Received project consistency completed event for application {}", event.getApplicationId());
        applicationService.processProjectConsistencyCompleted(event);
    }
}
