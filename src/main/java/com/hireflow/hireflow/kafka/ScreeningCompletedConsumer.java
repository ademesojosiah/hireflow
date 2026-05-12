package com.hireflow.hireflow.kafka;

import com.hireflow.hireflow.event.ScreeningCompletedEvent;
import com.hireflow.hireflow.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScreeningCompletedConsumer {

    private final ApplicationService applicationService;

    @KafkaListener(
            topics = "${hireflow.kafka.topics.screening-completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ScreeningCompletedEvent event) {
        log.info("Received screening completed event for application {}", event.getApplicationId());
        applicationService.processScreeningCompleted(event);
    }
}
