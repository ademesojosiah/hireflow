package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.CancelInterviewRequest;
import com.hireflow.hireflow.dto.request.RescheduleInterviewRequest;
import com.hireflow.hireflow.dto.request.ScheduleInterviewRequest;
import com.hireflow.hireflow.dto.response.InterviewSlotResponse;

public interface InterviewService {

    InterviewSlotResponse scheduleInterview(String applicationId, ScheduleInterviewRequest request, User user);

    InterviewSlotResponse rescheduleInterview(String applicationId, RescheduleInterviewRequest request, User user);

    InterviewSlotResponse cancelInterview(String applicationId, CancelInterviewRequest request, User user);

    InterviewSlotResponse getInterviewByApplication(String applicationId, User user);
}
