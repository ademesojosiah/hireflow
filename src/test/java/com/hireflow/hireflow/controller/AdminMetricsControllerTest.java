package com.hireflow.hireflow.controller;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.CompanyRepository;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.dto.response.ApplicationVolumeResponse;
import com.hireflow.hireflow.dto.response.TimeToHireResponse;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.AdminMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMetricsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private AdminMetricsService adminMetricsService;

    private Company company;
    private User admin;
    private User manager;
    private User applicant;

    @BeforeEach
    void setUp() {
        deleteAllData();

        company = saveCompany("Acme");
        admin = saveUser("alex.admin@example.com", Role.ADMIN, company);
        manager = saveUser("maya.manager@example.com", Role.HMANAGER, company);
        applicant = saveUser("ada.applicant@example.com", Role.APPLICANT, null);
    }

    @AfterEach
    void cleanUp() {
        reset(adminMetricsService);
        deleteAllData();
    }

    private void deleteAllData() {
        userRepository.deleteAll();
        companyRepository.deleteAll();
    }

    private Company saveCompany(String name) {
        Company c = new Company();
        c.setName(name);
        return companyRepository.save(c);
    }

    private User saveUser(String email, Role role, Company company) {
        User user = new User("Test", "User", email, passwordEncoder.encode("password123"), role, true);
        user.setCompany(company);
        return userRepository.save(user);
    }

    private UserPrincipal principalFor(User user) {
        return new UserPrincipal(user);
    }

    private static ApplicationVolumeResponse sampleVolume(String jobListingId) {
        Map<ApplicationStage, Long> map = new EnumMap<>(ApplicationStage.class);
        map.put(ApplicationStage.APPLIED, 12L);
        map.put(ApplicationStage.SCREENING, 47L);
        map.put(ApplicationStage.INTERVIEW_SCHEDULED, 8L);
        map.put(ApplicationStage.OFFER_SENT, 3L);
        map.put(ApplicationStage.HIRED, 11L);
        map.put(ApplicationStage.REJECTED, 25L);
        return new ApplicationVolumeResponse(map, 106L, jobListingId);
    }

    // ============================================================
    //   GET /api/v1/admin/metrics/application-volume
    // ============================================================

    @Test
    @DisplayName("application-volume: admin without filter returns 200 with all six stages and total")
    void volume_adminUnfiltered_returns200() throws Exception {
        when(adminMetricsService.getApplicationVolume(any(User.class), isNull()))
                .thenReturn(sampleVolume(null));

        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .with(user(principalFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application volume retrieved"))
                .andExpect(jsonPath("$.data.total").value(106))
                .andExpect(jsonPath("$.data.volumeByStage.APPLIED").value(12))
                .andExpect(jsonPath("$.data.volumeByStage.SCREENING").value(47))
                .andExpect(jsonPath("$.data.volumeByStage.INTERVIEW_SCHEDULED").value(8))
                .andExpect(jsonPath("$.data.volumeByStage.OFFER_SENT").value(3))
                .andExpect(jsonPath("$.data.volumeByStage.HIRED").value(11))
                .andExpect(jsonPath("$.data.volumeByStage.REJECTED").value(25))
                .andExpect(jsonPath("$.data.jobListingId").doesNotExist());
    }

    @Test
    @DisplayName("application-volume: forwards jobListingId query param to service and echoes it in response")
    void volume_withJobListingId_forwardsParam() throws Exception {
        when(adminMetricsService.getApplicationVolume(any(User.class), eq("job-77")))
                .thenReturn(sampleVolume("job-77"));

        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .param("jobListingId", "job-77")
                        .with(user(principalFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobListingId").value("job-77"));

        verify(adminMetricsService).getApplicationVolume(any(User.class), eq("job-77"));
    }

    @Test
    @DisplayName("application-volume: forbidden for HMANAGER")
    void volume_hmanager_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .with(user(principalFor(manager))))
                .andExpect(status().isForbidden());

        verify(adminMetricsService, never()).getApplicationVolume(any(), any());
    }

    @Test
    @DisplayName("application-volume: forbidden for APPLICANT")
    void volume_applicant_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .with(user(principalFor(applicant))))
                .andExpect(status().isForbidden());

        verify(adminMetricsService, never()).getApplicationVolume(any(), any());
    }

    @Test
    @DisplayName("application-volume: unauthenticated request is rejected")
    void volume_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/application-volume"))
                .andExpect(status().is4xxClientError());

        verify(adminMetricsService, never()).getApplicationVolume(any(), any());
    }

    @Test
    @DisplayName("application-volume: service-level AccessDeniedException surfaces as 403")
    void volume_serviceAccessDenied_returns403() throws Exception {
        when(adminMetricsService.getApplicationVolume(any(User.class), isNull()))
                .thenThrow(new AccessDeniedException("Only admins can perform this action"));

        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .with(user(principalFor(admin))))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    //   GET /api/v1/admin/metrics/time-to-hire
    // ============================================================

    @Test
    @DisplayName("time-to-hire: admin with data returns 200 and full stat block")
    void tth_adminWithData_returns200() throws Exception {
        when(adminMetricsService.getTimeToHire(any(User.class), isNull()))
                .thenReturn(new TimeToHireResponse(11, 432L, 408L, 720L, 168L, 912L, null));

        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire")
                        .with(user(principalFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Time to hire retrieved"))
                .andExpect(jsonPath("$.data.sampleSize").value(11))
                .andExpect(jsonPath("$.data.meanHours").value(432))
                .andExpect(jsonPath("$.data.medianHours").value(408))
                .andExpect(jsonPath("$.data.p95Hours").value(720))
                .andExpect(jsonPath("$.data.minHours").value(168))
                .andExpect(jsonPath("$.data.maxHours").value(912));
    }

    @Test
    @DisplayName("time-to-hire: admin with no hires returns sampleSize=0 and null stat fields")
    void tth_adminWithNoHires_returnsEmpty() throws Exception {
        when(adminMetricsService.getTimeToHire(any(User.class), isNull()))
                .thenReturn(new TimeToHireResponse(0, null, null, null, null, null, null));

        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire")
                        .with(user(principalFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sampleSize").value(0))
                .andExpect(jsonPath("$.data.meanHours").doesNotExist())
                .andExpect(jsonPath("$.data.medianHours").doesNotExist())
                .andExpect(jsonPath("$.data.p95Hours").doesNotExist())
                .andExpect(jsonPath("$.data.minHours").doesNotExist())
                .andExpect(jsonPath("$.data.maxHours").doesNotExist());
    }

    @Test
    @DisplayName("time-to-hire: forwards jobListingId filter to service")
    void tth_withJobListingId_forwardsParam() throws Exception {
        when(adminMetricsService.getTimeToHire(any(User.class), eq("job-42")))
                .thenReturn(new TimeToHireResponse(2, 200L, 200L, 300L, 100L, 300L, "job-42"));

        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire")
                        .param("jobListingId", "job-42")
                        .with(user(principalFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobListingId").value("job-42"))
                .andExpect(jsonPath("$.data.sampleSize").value(2));

        verify(adminMetricsService).getTimeToHire(any(User.class), eq("job-42"));
    }

    @Test
    @DisplayName("time-to-hire: forbidden for HMANAGER")
    void tth_hmanager_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire")
                        .with(user(principalFor(manager))))
                .andExpect(status().isForbidden());

        verify(adminMetricsService, never()).getTimeToHire(any(), any());
    }

    @Test
    @DisplayName("time-to-hire: forbidden for APPLICANT")
    void tth_applicant_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire")
                        .with(user(principalFor(applicant))))
                .andExpect(status().isForbidden());

        verify(adminMetricsService, never()).getTimeToHire(any(), any());
    }

    @Test
    @DisplayName("time-to-hire: unauthenticated request is rejected")
    void tth_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire"))
                .andExpect(status().is4xxClientError());

        verify(adminMetricsService, never()).getTimeToHire(any(), any());
    }

    @Test
    @DisplayName("time-to-hire: service-level AccessDeniedException surfaces as 403")
    void tth_serviceAccessDenied_returns403() throws Exception {
        when(adminMetricsService.getTimeToHire(any(User.class), isNull()))
                .thenThrow(new AccessDeniedException("Only admins can perform this action"));

        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire")
                        .with(user(principalFor(admin))))
                .andExpect(status().isForbidden());
    }
}
