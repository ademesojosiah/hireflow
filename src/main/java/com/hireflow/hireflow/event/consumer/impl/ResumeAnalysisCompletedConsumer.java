package com.hireflow.hireflow.event.consumer.impl;

import com.hireflow.hireflow.event.events.ResumeAnalysisCompletedEvent;
import com.hireflow.hireflow.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeAnalysisCompletedConsumer {

    private final ApplicationService applicationService;

    @KafkaListener(
            topics = "${hireflow.kafka.topics.resume-analysis-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type=com.hireflow.hireflow.event.events.ResumeAnalysisCompletedEvent"
            }
    )
    public void consume(ResumeAnalysisCompletedEvent event) {
        log.info("Received resume analysis completed event for application {}", event.getApplicationId());
        applicationService.processResumeAnalysisCompleted(event);
    }
}
