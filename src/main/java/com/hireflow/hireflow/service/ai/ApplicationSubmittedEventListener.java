package com.hireflow.hireflow.service.ai;

import com.hireflow.hireflow.event.ApplicationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationSubmittedEventListener {

    private final AiScreeningEventPublisher aiScreeningEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleApplicationSubmitted(ApplicationSubmittedEvent event) {
        aiScreeningEventPublisher.publishApplicationSubmitted(event);
    }
}
