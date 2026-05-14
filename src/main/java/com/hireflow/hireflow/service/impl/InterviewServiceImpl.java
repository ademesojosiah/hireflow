package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.InterviewSlot;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.CancelInterviewRequest;
import com.hireflow.hireflow.dto.request.RescheduleInterviewRequest;
import com.hireflow.hireflow.dto.request.ScheduleInterviewRequest;
import com.hireflow.hireflow.dto.response.InterviewSlotResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.event.producer.NotificationEventProducer;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.InterviewMapper;
import com.hireflow.hireflow.service.InterviewService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final UserService userService;
    private final InterviewSchedulingPersistence interviewSchedulingPersistence;
    private final NotificationEventProducer notificationEventProducer;
    private final InterviewMapper interviewMapper;

    @Override
    public InterviewSlotResponse scheduleInterview(String applicationId, ScheduleInterviewRequest request, User user) {
        try {
            User actor = requireCompanyManager(user);
            InterviewSlot slot = interviewSchedulingPersistence.schedule(applicationId, request, actor);
            notificationEventProducer.publishApplicationStageUpdate(
                    interviewScheduledEvent(slot, actor, "SCREENING", "Interview scheduled"));
            return interviewMapper.toResponse(slot);
        } catch (AccessDeniedException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Schedule interview failed for application {}: {}", applicationId, ex.getMessage());
            throw new CustomException("Schedule interview failed: Internal Server Error");
        }
    }

    @Override
    public InterviewSlotResponse rescheduleInterview(String applicationId, RescheduleInterviewRequest request, User user) {
        try {
            User actor = requireCompanyManager(user);
            InterviewSlot slot = interviewSchedulingPersistence.reschedule(applicationId, request, actor);
            // Stage stays at INTERVIEW_SCHEDULED; the notification carries the new time/link.
            notificationEventProducer.publishApplicationStageUpdate(
                    interviewScheduledEvent(slot, actor, "INTERVIEW_SCHEDULED", "Interview rescheduled"));
            return interviewMapper.toResponse(slot);
        } catch (AccessDeniedException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Reschedule interview failed for application {}: {}", applicationId, ex.getMessage());
            throw new CustomException("Reschedule interview failed: Internal Server Error");
        }
    }

    @Override
    public InterviewSlotResponse cancelInterview(String applicationId, CancelInterviewRequest request, User user) {
        try {
            User actor = requireCompanyManager(user);
            String reason = request == null ? null : request.getReason();
            InterviewSlot slot = interviewSchedulingPersistence.cancel(applicationId, reason, actor);
            // Cancellation moves the application back to SCREENING — publish a regular stage-change
            // notification so the applicant is informed.
            notificationEventProducer.publishApplicationStageUpdate(cancellationEvent(slot, actor, reason));
            return interviewMapper.toResponse(slot);
        } catch (AccessDeniedException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Cancel interview failed for application {}: {}", applicationId, ex.getMessage());
            throw new CustomException("Cancel interview failed: Internal Server Error");
        }
    }

    @Override
    public InterviewSlotResponse getInterviewByApplication(String applicationId, User user) {
        User actor = requireCompanyManager(user);
        InterviewSlot slot = interviewSchedulingPersistence.findActiveOrLatest(applicationId, actor);
        return interviewMapper.toResponse(slot);
    }

    private EmailNotificationEvent interviewScheduledEvent(InterviewSlot slot, User actor, String previousStage, String reason) {
        Application application = slot.getApplication();
        User applicant = application.getApplicant();
        JobListing job = application.getJobListing();
        if (applicant == null || applicant.getEmail() == null || applicant.getEmail().isBlank()) {
            throw new CustomException("Applicant is missing — cannot publish interview notification");
        }
        return EmailNotificationEvent.interviewScheduled(
                applicant.getEmail(),
                application.getId(),
                applicant.getId(),
                job == null ? null : job.getId(),
                job == null ? null : job.getTitle(),
                application.getCompanyId(),
                job == null || job.getCompany() == null ? null : job.getCompany().getName(),
                previousStage,
                reason,
                actor.getEmail(),
                "Your interview has been scheduled. Join via the meeting link at the scheduled time.",
                slot.getMeetingLink(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getTimezone(),
                slot.getInterviewerEmail()
        );
    }

    private EmailNotificationEvent cancellationEvent(InterviewSlot slot, User actor, String reason) {
        Application application = slot.getApplication();
        User applicant = application.getApplicant();
        JobListing job = application.getJobListing();
        if (applicant == null || applicant.getEmail() == null || applicant.getEmail().isBlank()) {
            throw new CustomException("Applicant is missing — cannot publish cancellation notification");
        }
        String effectiveReason = (reason == null || reason.isBlank())
                ? "Interview cancelled by " + actor.getEmail()
                : reason;
        return EmailNotificationEvent.applicationStageUpdated(
                applicant.getEmail(),
                application.getId(),
                applicant.getId(),
                job == null ? null : job.getId(),
                job == null ? null : job.getTitle(),
                application.getCompanyId(),
                job == null || job.getCompany() == null ? null : job.getCompany().getName(),
                "INTERVIEW_SCHEDULED",
                "SCREENING",
                effectiveReason,
                actor.getEmail(),
                "Your interview has been cancelled. The hiring team will follow up with next steps."
        );
    }

    private User requireCompanyManager(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        User refreshed = userService.findUserById(user.getId());
        if (refreshed == null || (refreshed.getRole() != Role.ADMIN && refreshed.getRole() != Role.HMANAGER)) {
            throw new AccessDeniedException("Only admins and hiring managers can manage interviews");
        }
        if (refreshed.getCompany() == null) {
            throw new AccessDeniedException("You must belong to a company to manage interviews");
        }
        return refreshed;
    }
}
