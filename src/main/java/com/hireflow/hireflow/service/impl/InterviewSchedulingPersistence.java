package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.InterviewSlot;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.data.repository.InterviewSlotRepository;
import com.hireflow.hireflow.dto.request.RescheduleInterviewRequest;
import com.hireflow.hireflow.dto.request.ScheduleInterviewRequest;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.InterviewStatus;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.restclient.MeetingLinkProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Transactional persistence for the interview lifecycle. Lives in its own bean so the
 * orchestrating service ({@link InterviewServiceImpl}) can publish Kafka notifications
 * AFTER the transaction commits — per BACKEND_RULES 8.2 (Transactional & Async Separation).
 */
@Component
@RequiredArgsConstructor
public class InterviewSchedulingPersistence {

    private static final Duration MIN_DURATION = Duration.ofMinutes(15);
    private static final Duration MAX_DURATION = Duration.ofHours(4);

    private final ApplicationRepository applicationRepository;
    private final InterviewSlotRepository interviewSlotRepository;
    private final MeetingLinkProvider meetingLinkProvider;

    @Transactional
    public InterviewSlot schedule(String applicationId, ScheduleInterviewRequest request, User actor) {
        Application application = loadApplicationForCompany(applicationId, actor);

        // Stage gate: an interview can only be scheduled out of SCREENING. INTERVIEW_SCHEDULED
        // means an active slot already exists; the caller should reschedule instead.
        if (application.getStage() != ApplicationStage.SCREENING) {
            throw new CustomException("Interviews can only be scheduled while the application is in SCREENING; current stage is " + application.getStage());
        }

        validateTimeSlot(request.getStartTime(), request.getEndTime(), request.getTimezone());

        // Defensive: in case stage and active-slot state ever drift, refuse to double-book.
        interviewSlotRepository.findByApplication_IdAndStatus(applicationId, InterviewStatus.SCHEDULED)
                .ifPresent(existing -> {
                    throw new CustomException("An interview is already scheduled for this application");
                });

        String meetingLink = meetingLinkProvider.createMeetingLink(
                application.getId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getTimezone(),
                request.getInterviewerEmail()
        );

        InterviewSlot slot = new InterviewSlot();
        slot.setApplication(application);
        slot.setCompanyId(application.getCompanyId());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setTimezone(request.getTimezone());
        slot.setMeetingProvider(meetingLinkProvider.provider());
        slot.setMeetingLink(meetingLink);
        slot.setInterviewerEmail(request.getInterviewerEmail());
        slot.setStatus(InterviewStatus.SCHEDULED);
        slot.setNotes(request.getNotes());

        InterviewSlot saved = interviewSlotRepository.save(slot);

        ApplicationStage previousStage = application.getStage();
        application.setStage(ApplicationStage.INTERVIEW_SCHEDULED);
        application.addStageUpdate(previousStage, ApplicationStage.INTERVIEW_SCHEDULED,
                "Interview scheduled", actor);
        applicationRepository.save(application);

        return saved;
    }

    @Transactional
    public InterviewSlot reschedule(String applicationId, RescheduleInterviewRequest request, User actor) {
        loadApplicationForCompany(applicationId, actor); // tenant ownership check

        InterviewSlot slot = interviewSlotRepository
                .findByApplication_IdAndStatus(applicationId, InterviewStatus.SCHEDULED)
                .orElseThrow(() -> new ResourceNotFoundException("No active interview to reschedule"));

        validateTimeSlot(request.getStartTime(), request.getEndTime(), request.getTimezone());

        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setTimezone(request.getTimezone());
        if (request.getInterviewerEmail() != null && !request.getInterviewerEmail().isBlank()) {
            slot.setInterviewerEmail(request.getInterviewerEmail());
        }
        if (request.getNotes() != null) {
            slot.setNotes(request.getNotes());
        }
        // Meeting link is preserved on reschedule — Meet links remain valid for the same calendar event.
        // Re-issue would invalidate any external calendar invites already accepted.

        return interviewSlotRepository.save(slot);
    }

    @Transactional
    public InterviewSlot cancel(String applicationId, String reason, User actor) {
        Application application = loadApplicationForCompany(applicationId, actor);

        InterviewSlot slot = interviewSlotRepository
                .findByApplication_IdAndStatus(applicationId, InterviewStatus.SCHEDULED)
                .orElseThrow(() -> new ResourceNotFoundException("No active interview to cancel"));

        slot.setStatus(InterviewStatus.CANCELLED);
        interviewSlotRepository.save(slot);

        ApplicationStage previousStage = application.getStage();
        if (previousStage == ApplicationStage.INTERVIEW_SCHEDULED) {
            application.setStage(ApplicationStage.SCREENING);
            String reasonText = (reason == null || reason.isBlank())
                    ? "Interview cancelled by " + actor.getEmail()
                    : reason;
            application.addStageUpdate(previousStage, ApplicationStage.SCREENING, reasonText, actor);
            applicationRepository.save(application);
        }

        return slot;
    }

    @Transactional(readOnly = true)
    public InterviewSlot findActiveOrLatest(String applicationId, User actor) {
        loadApplicationForCompanyReadOnly(applicationId, actor);
        return interviewSlotRepository
                .findByApplication_IdAndStatus(applicationId, InterviewStatus.SCHEDULED)
                .or(() -> interviewSlotRepository.findTopByApplication_IdAndCompanyIdOrderByCreatedAtDesc(
                        applicationId, actor.getCompany().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No interview found for this application"));
    }

    @Transactional(readOnly = true)
    public InterviewSlot findActiveOrLatestForApplicant(String applicationId, User applicant) {
        Application application = applicationRepository.findByIdAndApplicant_Id(applicationId, applicant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return interviewSlotRepository
                .findByApplication_IdAndStatus(applicationId, InterviewStatus.SCHEDULED)
                .or(() -> interviewSlotRepository.findTopByApplication_IdAndCompanyIdOrderByCreatedAtDesc(
                        applicationId, application.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException("No interview found for this application"));
    }

    private Application loadApplicationForCompany(String applicationId, User actor) {
        return applicationRepository.findByIdAndCompanyIdForUpdate(applicationId, actor.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private Application loadApplicationForCompanyReadOnly(String applicationId, User actor) {
        return applicationRepository.findByIdAndCompanyId(applicationId, actor.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private void validateTimeSlot(Instant start, Instant end, String timezone) {
        if (!end.isAfter(start)) {
            throw new CustomException("endTime must be after startTime");
        }
        Duration duration = Duration.between(start, end);
        if (duration.compareTo(MIN_DURATION) < 0) {
            throw new CustomException("Interview must be at least 15 minutes long");
        }
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw new CustomException("Interview cannot exceed 4 hours");
        }
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            throw new CustomException("Unknown timezone: " + timezone);
        }
    }
}
