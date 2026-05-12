package com.hireflow.hireflow.service.ai;

import com.hireflow.hireflow.event.ApplicationSubmittedEvent;

public interface AiScreeningEventPublisher {
    void publishApplicationSubmitted(ApplicationSubmittedEvent event);
}
