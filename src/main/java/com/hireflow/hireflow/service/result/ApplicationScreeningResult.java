package com.hireflow.hireflow.service.result;

import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplicationScreeningResult {

    private EmailNotificationEvent notificationEvent;
}
