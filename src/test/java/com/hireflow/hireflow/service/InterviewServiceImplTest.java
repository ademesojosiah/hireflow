package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Application;
import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.InterviewSlot;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.data.repository.InterviewSlotRepository;
import com.hireflow.hireflow.dto.request.CancelInterviewRequest;
import com.hireflow.hireflow.dto.request.ScheduleInterviewRequest;
import com.hireflow.hireflow.dto.response.InterviewSlotResponse;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.InterviewStatus;
import com.hireflow.hireflow.enums.MeetingProvider;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.event.producer.NotificationEventProducer;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.mapper.InterviewMapper;
import com.hireflow.hireflow.restclient.MeetingLinkProvider;
import com.hireflow.hireflow.service.impl.InterviewSchedulingPersistence;
import com.hireflow.hireflow.service.impl.InterviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private InterviewSlotRepository interviewSlotRepository;
    @Mock private MeetingLinkProvider meetingLinkProvider;
    @Mock private NotificationEventProducer notificationEventProducer;
    @Mock private UserService userService;

    private InterviewServiceImpl interviewService;

    private Company company;
    private User applicant;
    private User manager;
    private JobListing job;
    private Application application;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId("company-1");
        company.setName("Acme");

        applicant = new User("Ada", "Applicant", "ada@example.com", "password", Role.APPLICANT, true);
        applicant.setId("applicant-1");

        manager = new User("Maya", "Manager", "maya@example.com", "password", Role.HMANAGER, true);
        manager.setId("manager-1");
        manager.setCompany(company);

        job = new JobListing();
        job.setId("job-1");
        job.setTitle("Backend Engineer");
        job.setCompany(company);

        application = new Application();
        application.setId("application-1");
        application.setApplicant(applicant);
        application.setJobListing(job);
        application.setCompanyId(company.getId());
        application.setStage(ApplicationStage.SCREENING);

        InterviewSchedulingPersistence persistence = new InterviewSchedulingPersistence(
                applicationRepository, interviewSlotRepository, meetingLinkProvider
        );
        interviewService = new InterviewServiceImpl(
                userService, persistence, notificationEventProducer, new InterviewMapper()
        );
    }

    @Test
    @DisplayName("Should schedule an interview, generate a Meet link, transition SCREENING → INTERVIEW_SCHEDULED, and notify the applicant")
    void scheduleInterview_happyPath() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyIdForUpdate(application.getId(), company.getId()))
                .thenReturn(Optional.of(application));
        when(interviewSlotRepository.findByApplication_IdAndStatus(application.getId(), InterviewStatus.SCHEDULED))
                .thenReturn(Optional.empty());
        when(meetingLinkProvider.provider()).thenReturn(MeetingProvider.GOOGLE_MEET);
        when(meetingLinkProvider.createMeetingLink(any(), any(), any(), any(), any()))
                .thenReturn("https://meet.google.com/abc-defg-hij");
        when(interviewSlotRepository.save(any(InterviewSlot.class))).thenAnswer(invocation -> {
            InterviewSlot slot = invocation.getArgument(0);
            slot.setId("slot-1");
            return slot;
        });

        ScheduleInterviewRequest request = scheduleRequest();

        InterviewSlotResponse response = interviewService.scheduleInterview(application.getId(), request, manager);

        assertThat(response.getId()).isEqualTo("slot-1");
        assertThat(response.getMeetingLink()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(response.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        assertThat(application.getStage()).isEqualTo(ApplicationStage.INTERVIEW_SCHEDULED);
        assertThat(application.getStageUpdates()).hasSize(1);
        assertThat(application.getStageUpdates().getFirst().getCurrentStage())
                .isEqualTo(ApplicationStage.INTERVIEW_SCHEDULED);
        assertThat(application.getStageUpdates().getFirst().getActorId()).isEqualTo(manager.getId());
        assertThat(application.getStageUpdates().getFirst().getActorEmail()).isEqualTo(manager.getEmail());
        assertThat(application.getStageUpdates().getFirst().getActorRole()).isEqualTo(Role.HMANAGER);

        // The notification must carry the meeting link, time slot, and applicant routing fields.
        verify(notificationEventProducer).publishApplicationStageUpdate(argThat(notification ->
                notification != null
                        && applicant.getEmail().equals(notification.getTo())
                        && applicant.getId().equals(notification.getApplicantId())
                        && "INTERVIEW_SCHEDULED".equals(notification.getCurrentStage())
                        && "SCREENING".equals(notification.getPreviousStage())
                        && "https://meet.google.com/abc-defg-hij".equals(notification.getMeetingLink())
                        && notification.getInterviewStartTime() != null
                        && notification.getInterviewEndTime() != null
                        && "America/Los_Angeles".equals(notification.getInterviewTimezone())
        ));
    }

    @Test
    @DisplayName("Should refuse to schedule when application is not in SCREENING")
    void scheduleInterview_wrongStage() {
        application.setStage(ApplicationStage.INTERVIEW_SCHEDULED);
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyIdForUpdate(application.getId(), company.getId()))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> interviewService.scheduleInterview(application.getId(), scheduleRequest(), manager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Interviews can only be scheduled while the application is in SCREENING");

        verify(interviewSlotRepository, never()).save(any());
        verifyNoInteractions(notificationEventProducer);
    }

    @Test
    @DisplayName("Should refuse to schedule when an active interview slot already exists")
    void scheduleInterview_doubleBook() {
        InterviewSlot existing = new InterviewSlot();
        existing.setId("slot-existing");
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyIdForUpdate(application.getId(), company.getId()))
                .thenReturn(Optional.of(application));
        when(interviewSlotRepository.findByApplication_IdAndStatus(application.getId(), InterviewStatus.SCHEDULED))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> interviewService.scheduleInterview(application.getId(), scheduleRequest(), manager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("already scheduled");

        verify(interviewSlotRepository, never()).save(any());
        verifyNoInteractions(notificationEventProducer);
    }

    @Test
    @DisplayName("Should refuse to schedule when endTime is not after startTime")
    void scheduleInterview_invalidWindow() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyIdForUpdate(application.getId(), company.getId()))
                .thenReturn(Optional.of(application));

        ScheduleInterviewRequest request = scheduleRequest();
        request.setEndTime(request.getStartTime()); // zero-length window

        assertThatThrownBy(() -> interviewService.scheduleInterview(application.getId(), request, manager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    @DisplayName("Should refuse to schedule when timezone id is unknown")
    void scheduleInterview_invalidTimezone() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyIdForUpdate(application.getId(), company.getId()))
                .thenReturn(Optional.of(application));

        ScheduleInterviewRequest request = scheduleRequest();
        request.setTimezone("Not/A_Real_Zone");

        assertThatThrownBy(() -> interviewService.scheduleInterview(application.getId(), request, manager))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Unknown timezone");
    }

    @Test
    @DisplayName("Should refuse interview operations from an applicant role")
    void scheduleInterview_forbiddenForApplicant() {
        when(userService.findUserById(applicant.getId())).thenReturn(applicant);

        assertThatThrownBy(() -> interviewService.scheduleInterview(application.getId(), scheduleRequest(), applicant))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("admins and hiring managers");
    }

    @Test
    @DisplayName("Should cancel an active interview, move stage back to SCREENING, and notify the applicant")
    void cancelInterview_movesBackToScreening() {
        application.setStage(ApplicationStage.INTERVIEW_SCHEDULED);
        InterviewSlot slot = activeSlot();
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(applicationRepository.findByIdAndCompanyIdForUpdate(application.getId(), company.getId()))
                .thenReturn(Optional.of(application));
        when(interviewSlotRepository.findByApplication_IdAndStatus(application.getId(), InterviewStatus.SCHEDULED))
                .thenReturn(Optional.of(slot));
        when(interviewSlotRepository.save(any(InterviewSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewSlotResponse response = interviewService.cancelInterview(
                application.getId(), new CancelInterviewRequest("Candidate requested reschedule"), manager);

        assertThat(response.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
        assertThat(application.getStage()).isEqualTo(ApplicationStage.SCREENING);
        assertThat(application.getStageUpdates()).hasSize(1);
        verify(notificationEventProducer).publishApplicationStageUpdate(argThat(notification ->
                notification != null
                        && applicant.getEmail().equals(notification.getTo())
                        && "SCREENING".equals(notification.getCurrentStage())
                        && "INTERVIEW_SCHEDULED".equals(notification.getPreviousStage())
        ));
    }

    private ScheduleInterviewRequest scheduleRequest() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        return new ScheduleInterviewRequest(start, end, "America/Los_Angeles", "maya@example.com", "Round 1 technical");
    }

    private InterviewSlot activeSlot() {
        InterviewSlot slot = new InterviewSlot();
        slot.setId("slot-1");
        slot.setApplication(application);
        slot.setCompanyId(company.getId());
        slot.setStartTime(Instant.now().plus(2, ChronoUnit.DAYS));
        slot.setEndTime(Instant.now().plus(2, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS));
        slot.setTimezone("America/Los_Angeles");
        slot.setMeetingProvider(MeetingProvider.GOOGLE_MEET);
        slot.setMeetingLink("https://meet.google.com/abc-defg-hij");
        slot.setInterviewerEmail("maya@example.com");
        slot.setStatus(InterviewStatus.SCHEDULED);
        return slot;
    }
}
