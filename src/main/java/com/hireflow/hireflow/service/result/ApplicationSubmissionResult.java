package com.hireflow.hireflow.service.result;

import com.hireflow.hireflow.dto.response.ApplicationResponse;
import com.hireflow.hireflow.event.events.ApplicationSubmittedEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplicationSubmissionResult {

    private ApplicationResponse response;
    private ApplicationSubmittedEvent event;
}
