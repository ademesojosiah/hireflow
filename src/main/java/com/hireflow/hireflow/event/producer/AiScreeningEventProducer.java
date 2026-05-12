package com.hireflow.hireflow.event.producer;

import com.hireflow.hireflow.event.events.ApplicationSubmittedEvent;

public interface AiScreeningEventProducer {

    void publishApplicationSubmittedAsync(ApplicationSubmittedEvent event);

    void publishApplicationSubmitted(ApplicationSubmittedEvent event);
}
