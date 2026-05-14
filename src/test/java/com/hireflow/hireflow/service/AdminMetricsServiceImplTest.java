package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.data.repository.projection.StageVolumeProjection;
import com.hireflow.hireflow.data.repository.projection.TimeToHireProjection;
import com.hireflow.hireflow.dto.response.ApplicationVolumeResponse;
import com.hireflow.hireflow.dto.response.TimeToHireResponse;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.service.impl.AdminMetricsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMetricsServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private UserService userService;

    private AdminMetricsServiceImpl adminMetricsService;

    private Company company;
    private User admin;
    private User applicant;
    private User manager;

    @BeforeEach
    void setUp() {
        adminMetricsService = new AdminMetricsServiceImpl(applicationRepository, userService);

        company = new Company();
        company.setId("company-1");
        company.setName("Acme");

        admin = new User("Alex", "Admin", "alex@example.com", "password", Role.ADMIN, true);
        admin.setId("admin-1");
        admin.setCompany(company);

        applicant = new User("Ada", "Applicant", "ada@example.com", "password", Role.APPLICANT, true);
        applicant.setId("applicant-1");

        manager = new User("Maya", "Manager", "maya@example.com", "password", Role.HMANAGER, true);
        manager.setId("manager-1");
        manager.setCompany(company);
    }

    // ---------- shared test helpers ----------

    private static StageVolumeProjection stageRow(ApplicationStage stage, long count) {
        return new StageVolumeRow(stage, count);
    }

    private static TimeToHireProjection durationRow(Instant appliedAt, Instant hiredAt) {
        return new TimeToHireRow(appliedAt, hiredAt);
    }

    private record StageVolumeRow(ApplicationStage stage, Long count) implements StageVolumeProjection {
        @Override
        public ApplicationStage getStage() {
            return stage;
        }

        @Override
        public Long getCount() {
            return count;
        }
    }

    private record TimeToHireRow(Instant appliedAt, Instant hiredAt) implements TimeToHireProjection {
        @Override
        public Instant getAppliedAt() {
            return appliedAt;
        }

        @Override
        public Instant getHiredAt() {
            return hiredAt;
        }
    }

    private static Instant t(long hoursAfterEpoch) {
        return Instant.ofEpochSecond(hoursAfterEpoch * 3600L);
    }

    // ============================================================
    //   getApplicationVolume
    // ============================================================

    @Nested
    @DisplayName("getApplicationVolume")
    class GetApplicationVolume {

        @Test
        @DisplayName("Returns counts for every stage, filling missing stages with zero")
        void returnsCountsForEveryStage_FillingMissingWithZero() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.countByStageForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            stageRow(ApplicationStage.SCREENING, 4L),
                            stageRow(ApplicationStage.HIRED, 2L)
                    ));

            ApplicationVolumeResponse response = adminMetricsService.getApplicationVolume(admin, null);

            assertThat(response.getTotal()).isEqualTo(6L);
            assertThat(response.getJobListingId()).isNull();
            assertThat(response.getVolumeByStage())
                    .containsEntry(ApplicationStage.APPLIED, 0L)
                    .containsEntry(ApplicationStage.SCREENING, 4L)
                    .containsEntry(ApplicationStage.INTERVIEW_SCHEDULED, 0L)
                    .containsEntry(ApplicationStage.OFFER_SENT, 0L)
                    .containsEntry(ApplicationStage.HIRED, 2L)
                    .containsEntry(ApplicationStage.REJECTED, 0L);
        }

        @Test
        @DisplayName("Returns zero for every stage when company has no applications")
        void returnsAllZeros_WhenNoApplications() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.countByStageForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of());

            ApplicationVolumeResponse response = adminMetricsService.getApplicationVolume(admin, null);

            assertThat(response.getTotal()).isZero();
            assertThat(response.getVolumeByStage())
                    .hasSize(ApplicationStage.values().length)
                    .allSatisfy((stage, count) -> assertThat(count).isZero());
        }

        @Test
        @DisplayName("Handles every stage populated and totals correctly")
        void totalsAllStagesCorrectly() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.countByStageForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            stageRow(ApplicationStage.APPLIED, 12L),
                            stageRow(ApplicationStage.SCREENING, 47L),
                            stageRow(ApplicationStage.INTERVIEW_SCHEDULED, 8L),
                            stageRow(ApplicationStage.OFFER_SENT, 3L),
                            stageRow(ApplicationStage.HIRED, 11L),
                            stageRow(ApplicationStage.REJECTED, 25L)
                    ));

            ApplicationVolumeResponse response = adminMetricsService.getApplicationVolume(admin, null);

            assertThat(response.getTotal()).isEqualTo(106L);
            assertThat(response.getVolumeByStage().get(ApplicationStage.SCREENING)).isEqualTo(47L);
        }

        @Test
        @DisplayName("Tolerates null count in projection by treating it as zero")
        void tolerantOfNullCountInProjection() {
            StageVolumeProjection nullCountRow = new StageVolumeRow(ApplicationStage.APPLIED, null);

            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.countByStageForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(nullCountRow));

            ApplicationVolumeResponse response = adminMetricsService.getApplicationVolume(admin, null);

            assertThat(response.getTotal()).isZero();
            assertThat(response.getVolumeByStage().get(ApplicationStage.APPLIED)).isZero();
        }

        @Test
        @DisplayName("Forwards jobListingId filter to repository and echoes it in response")
        void forwardsJobListingIdFilter() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.countByStageForCompany("company-1", "job-99"))
                    .thenReturn(List.of(stageRow(ApplicationStage.SCREENING, 1L)));

            ApplicationVolumeResponse response = adminMetricsService.getApplicationVolume(admin, "job-99");

            assertThat(response.getJobListingId()).isEqualTo("job-99");
            assertThat(response.getTotal()).isEqualTo(1L);
            verify(applicationRepository).countByStageForCompany("company-1", "job-99");
        }

        @Test
        @DisplayName("Uses companyId resolved from the admin loaded by userService, not from the caller argument")
        void usesCompanyIdFromUserService() {
            User staleAdmin = new User("Alex", "Admin", "alex@example.com", "password", Role.ADMIN, true);
            staleAdmin.setId("admin-1");
            Company otherCompany = new Company();
            otherCompany.setId("company-2");
            staleAdmin.setCompany(otherCompany);

            when(userService.findUserById("admin-1")).thenReturn(admin);
            when(applicationRepository.countByStageForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of());

            adminMetricsService.getApplicationVolume(staleAdmin, null);

            ArgumentCaptor<String> companyIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(applicationRepository).countByStageForCompany(companyIdCaptor.capture(), isNull());
            assertThat(companyIdCaptor.getValue()).isEqualTo("company-1");
        }

        @Test
        @DisplayName("Throws AccessDeniedException when caller is null")
        void rejectsNullCaller() {
            assertThatThrownBy(() -> adminMetricsService.getApplicationVolume(null, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Throws AccessDeniedException for APPLICANT role")
        void rejectsApplicant() {
            when(userService.findUserById(applicant.getId())).thenReturn(applicant);

            assertThatThrownBy(() -> adminMetricsService.getApplicationVolume(applicant, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Throws AccessDeniedException for HMANAGER role")
        void rejectsHManager() {
            when(userService.findUserById(manager.getId())).thenReturn(manager);

            assertThatThrownBy(() -> adminMetricsService.getApplicationVolume(manager, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Throws AccessDeniedException when admin is no longer in the system")
        void rejectsCallerNotInDb() {
            when(userService.findUserById(admin.getId())).thenReturn(null);

            assertThatThrownBy(() -> adminMetricsService.getApplicationVolume(admin, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when admin has no company")
        void rejectsAdminWithoutCompany() {
            admin.setCompany(null);
            when(userService.findUserById(admin.getId())).thenReturn(admin);

            assertThatThrownBy(() -> adminMetricsService.getApplicationVolume(admin, null))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(applicationRepository);
        }
    }

    // ============================================================
    //   getTimeToHire
    // ============================================================

    @Nested
    @DisplayName("getTimeToHire")
    class GetTimeToHire {

        @Test
        @DisplayName("Returns empty stats when no hired applications")
        void returnsEmptyStats_WhenNoHires() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of());

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getSampleSize()).isZero();
            assertThat(response.getMeanHours()).isNull();
            assertThat(response.getMedianHours()).isNull();
            assertThat(response.getP95Hours()).isNull();
            assertThat(response.getMinHours()).isNull();
            assertThat(response.getMaxHours()).isNull();
            assertThat(response.getJobListingId()).isNull();
        }

        @Test
        @DisplayName("Single hire: mean, median, p95, min, max all equal that one duration")
        void singleHire_AllStatsEqualThatDuration() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(durationRow(t(0), t(72)))); // 72 hours

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getSampleSize()).isEqualTo(1);
            assertThat(response.getMeanHours()).isEqualTo(72L);
            assertThat(response.getMedianHours()).isEqualTo(72L);
            assertThat(response.getP95Hours()).isEqualTo(72L);
            assertThat(response.getMinHours()).isEqualTo(72L);
            assertThat(response.getMaxHours()).isEqualTo(72L);
        }

        @Test
        @DisplayName("Computes mean as integer rounded average of durations in hours")
        void computesMeanCorrectly() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            durationRow(t(0), t(100)),
                            durationRow(t(0), t(200)),
                            durationRow(t(0), t(300))
                    ));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getMeanHours()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Computes min and max correctly across many samples")
        void computesMinAndMax() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            durationRow(t(0), t(50)),
                            durationRow(t(0), t(900)),
                            durationRow(t(0), t(200)),
                            durationRow(t(0), t(450))
                    ));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getMinHours()).isEqualTo(50L);
            assertThat(response.getMaxHours()).isEqualTo(900L);
        }

        @Test
        @DisplayName("Computes median (nearest-rank) correctly on odd sample size")
        void computesMedian_OddSize() {
            // sorted: 100, 200, 300, 400, 500 → ceil(0.5*5)-1 = 2 → element[2] = 300
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            durationRow(t(0), t(500)),
                            durationRow(t(0), t(100)),
                            durationRow(t(0), t(400)),
                            durationRow(t(0), t(200)),
                            durationRow(t(0), t(300))
                    ));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getMedianHours()).isEqualTo(300L);
        }

        @Test
        @DisplayName("Computes median (nearest-rank) correctly on even sample size")
        void computesMedian_EvenSize() {
            // sorted: 100,200,300,400 → ceil(0.5*4)-1 = 1 → element[1] = 200
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            durationRow(t(0), t(400)),
                            durationRow(t(0), t(200)),
                            durationRow(t(0), t(100)),
                            durationRow(t(0), t(300))
                    ));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getMedianHours()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Computes p95 correctly with 20 samples (sorted 10..200 step 10)")
        void computesP95_With20Samples() {
            // sorted [10,20,...,200] → ceil(0.95*20)-1 = 19-1 = 18 → element[18] = 190
            List<TimeToHireProjection> rows = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                rows.add(durationRow(t(0), t(i * 10L)));
            }
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(rows);

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getP95Hours()).isEqualTo(190L);
            assertThat(response.getMinHours()).isEqualTo(10L);
            assertThat(response.getMaxHours()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Skips rows with null appliedAt")
        void skipsRowsWithNullAppliedAt() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            durationRow(null, t(100)),
                            durationRow(t(0), t(200))
                    ));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getSampleSize()).isEqualTo(1);
            assertThat(response.getMeanHours()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Skips rows with null hiredAt")
        void skipsRowsWithNullHiredAt() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            durationRow(t(0), null),
                            durationRow(t(0), t(150))
                    ));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getSampleSize()).isEqualTo(1);
            assertThat(response.getMeanHours()).isEqualTo(150L);
        }

        @Test
        @DisplayName("Skips rows where hiredAt equals appliedAt (zero duration is excluded)")
        void skipsZeroDurationRows() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            durationRow(t(0), t(0)),
                            durationRow(t(0), t(80))
                    ));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getSampleSize()).isEqualTo(1);
            assertThat(response.getMeanHours()).isEqualTo(80L);
        }

        @Test
        @DisplayName("Skips rows where hiredAt is before appliedAt (data corruption guard)")
        void skipsInvertedDurationRows() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            durationRow(t(100), t(50)), // inverted
                            durationRow(t(0), t(80))
                    ));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getSampleSize()).isEqualTo(1);
            assertThat(response.getMeanHours()).isEqualTo(80L);
        }

        @Test
        @DisplayName("Returns empty stats when every row is invalid")
        void returnsEmpty_WhenEveryRowInvalid() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(
                            durationRow(null, t(50)),
                            durationRow(t(0), null),
                            durationRow(t(100), t(50))
                    ));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            assertThat(response.getSampleSize()).isZero();
            assertThat(response.getMeanHours()).isNull();
        }

        @Test
        @DisplayName("Computes durations in hours, truncating fractional hours")
        void truncatesFractionalHours() {
            // 1.5 hours = 90 minutes = 5400 seconds
            Instant from = Instant.ofEpochSecond(0);
            Instant to = Instant.ofEpochSecond(5400);

            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                    .thenReturn(List.of(durationRow(from, to)));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, null);

            // Duration.toHours() truncates → 1 hour
            assertThat(response.getMeanHours()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Forwards jobListingId to repository and echoes it in response")
        void forwardsJobListingIdFilter() {
            when(userService.findUserById(admin.getId())).thenReturn(admin);
            when(applicationRepository.findHiredDurationsForCompany("company-1", "job-77"))
                    .thenReturn(List.of(durationRow(t(0), t(100))));

            TimeToHireResponse response = adminMetricsService.getTimeToHire(admin, "job-77");

            assertThat(response.getJobListingId()).isEqualTo("job-77");
            verify(applicationRepository).findHiredDurationsForCompany("company-1", "job-77");
        }

        @Test
        @DisplayName("Throws AccessDeniedException when caller is null")
        void rejectsNullCaller() {
            assertThatThrownBy(() -> adminMetricsService.getTimeToHire(null, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Throws AccessDeniedException for APPLICANT role")
        void rejectsApplicant() {
            when(userService.findUserById(applicant.getId())).thenReturn(applicant);

            assertThatThrownBy(() -> adminMetricsService.getTimeToHire(applicant, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Throws AccessDeniedException for HMANAGER role")
        void rejectsHManager() {
            when(userService.findUserById(manager.getId())).thenReturn(manager);

            assertThatThrownBy(() -> adminMetricsService.getTimeToHire(manager, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Throws AccessDeniedException when caller is gone from the user store")
        void rejectsCallerNotInDb() {
            when(userService.findUserById(admin.getId())).thenReturn(null);

            assertThatThrownBy(() -> adminMetricsService.getTimeToHire(admin, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when admin has no company")
        void rejectsAdminWithoutCompany() {
            admin.setCompany(null);
            when(userService.findUserById(admin.getId())).thenReturn(admin);

            assertThatThrownBy(() -> adminMetricsService.getTimeToHire(admin, null))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoInteractions(applicationRepository);
        }
    }
}
