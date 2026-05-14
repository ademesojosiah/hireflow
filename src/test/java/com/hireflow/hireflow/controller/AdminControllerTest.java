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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
class AdminControllerTest {

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
        admin = saveUser("alex@example.com", Role.ADMIN, company);
        manager = saveUser("maya@example.com", Role.HMANAGER, company);
        applicant = saveUser("ada@example.com", Role.APPLICANT, null);
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

    private User saveUser(String email, Role role, Company c) {
        User user = new User("Test", "User", email, passwordEncoder.encode("password123"), role, true);
        user.setCompany(c);
        return userRepository.save(user);
    }

    private UserPrincipal principalFor(User user) {
        return new UserPrincipal(user);
    }

    // ============================================================
    //   /metrics/application-volume
    // ============================================================

    @Test
    @DisplayName("Admin can fetch application volume without a job filter")
    void getVolume_admin_noFilter() throws Exception {
        Map<ApplicationStage, Long> volumeByStage = new EnumMap<>(ApplicationStage.class);
        volumeByStage.put(ApplicationStage.APPLIED, 12L);
        volumeByStage.put(ApplicationStage.SCREENING, 47L);
        volumeByStage.put(ApplicationStage.INTERVIEW_SCHEDULED, 8L);
        volumeByStage.put(ApplicationStage.OFFER_SENT, 3L);
        volumeByStage.put(ApplicationStage.HIRED, 11L);
        volumeByStage.put(ApplicationStage.REJECTED, 25L);

        when(adminMetricsService.getApplicationVolume(any(User.class), isNull()))
                .thenReturn(new ApplicationVolumeResponse(volumeByStage, 106L, null));

        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .with(user(principalFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Application volume retrieved"))
                .andExpect(jsonPath("$.data.total").value(106))
                .andExpect(jsonPath("$.data.volumeByStage.APPLIED").value(12))
                .andExpect(jsonPath("$.data.volumeByStage.SCREENING").value(47))
                .andExpect(jsonPath("$.data.volumeByStage.HIRED").value(11))
                .andExpect(jsonPath("$.data.volumeByStage.REJECTED").value(25))
                .andExpect(jsonPath("$.data.jobListingId").doesNotExist());

        verify(adminMetricsService).getApplicationVolume(any(User.class), isNull());
    }

    @Test
    @DisplayName("Admin can fetch application volume filtered by jobListingId")
    void getVolume_admin_withFilter() throws Exception {
        Map<ApplicationStage, Long> volumeByStage = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage s : ApplicationStage.values()) {
            volumeByStage.put(s, 0L);
        }
        volumeByStage.put(ApplicationStage.SCREENING, 5L);
        volumeByStage.put(ApplicationStage.REJECTED, 1L);

        when(adminMetricsService.getApplicationVolume(any(User.class), eq("job-abc-123")))
                .thenReturn(new ApplicationVolumeResponse(volumeByStage, 6L, "job-abc-123"));

        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .param("jobListingId", "job-abc-123")
                        .with(user(principalFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(6))
                .andExpect(jsonPath("$.data.jobListingId").value("job-abc-123"))
                .andExpect(jsonPath("$.data.volumeByStage.SCREENING").value(5))
                .andExpect(jsonPath("$.data.volumeByStage.APPLIED").value(0));

        verify(adminMetricsService).getApplicationVolume(any(User.class), eq("job-abc-123"));
    }

    @Test
    @DisplayName("Application volume returns zeros for every stage when company has no applications")
    void getVolume_emptyCompany() throws Exception {
        Map<ApplicationStage, Long> volumeByStage = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage s : ApplicationStage.values()) {
            volumeByStage.put(s, 0L);
        }

        when(adminMetricsService.getApplicationVolume(any(User.class), isNull()))
                .thenReturn(new ApplicationVolumeResponse(volumeByStage, 0L, null));

        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .with(user(principalFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.volumeByStage.APPLIED").value(0))
                .andExpect(jsonPath("$.data.volumeByStage.HIRED").value(0));
    }

    @Test
    @DisplayName("HManager cannot access application volume endpoint")
    void getVolume_hmanager_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .with(user(principalFor(manager))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Applicant cannot access application volume endpoint")
    void getVolume_applicant_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/application-volume")
                        .with(user(principalFor(applicant))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to application volume endpoint is rejected")
    void getVolume_unauthenticated_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/application-volume"))
                .andExpect(status().is4xxClientError());
    }

    // ============================================================
    //   /metrics/time-to-hire
    // ============================================================

    @Test
    @DisplayName("Admin can fetch time-to-hire stats without a job filter")
    void getTimeToHire_admin_noFilter() throws Exception {
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
                .andExpect(jsonPath("$.data.maxHours").value(912))
                .andExpect(jsonPath("$.data.jobListingId").doesNotExist());

        verify(adminMetricsService).getTimeToHire(any(User.class), isNull());
    }

    @Test
    @DisplayName("Admin can fetch time-to-hire stats filtered by jobListingId")
    void getTimeToHire_admin_withFilter() throws Exception {
        when(adminMetricsService.getTimeToHire(any(User.class), eq("job-xyz")))
                .thenReturn(new TimeToHireResponse(3, 240L, 240L, 360L, 120L, 360L, "job-xyz"));

        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire")
                        .param("jobListingId", "job-xyz")
                        .with(user(principalFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sampleSize").value(3))
                .andExpect(jsonPath("$.data.jobListingId").value("job-xyz"))
                .andExpect(jsonPath("$.data.meanHours").value(240))
                .andExpect(jsonPath("$.data.maxHours").value(360));

        verify(adminMetricsService).getTimeToHire(any(User.class), eq("job-xyz"));
    }

    @Test
    @DisplayName("Time-to-hire returns sampleSize 0 with explicit null stats when no hires exist")
    void getTimeToHire_noHires() throws Exception {
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
    @DisplayName("HManager cannot access time-to-hire endpoint")
    void getTimeToHire_hmanager_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire")
                        .with(user(principalFor(manager))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Applicant cannot access time-to-hire endpoint")
    void getTimeToHire_applicant_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire")
                        .with(user(principalFor(applicant))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to time-to-hire endpoint is rejected")
    void getTimeToHire_unauthenticated_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metrics/time-to-hire"))
                .andExpect(status().is4xxClientError());
    }
}
