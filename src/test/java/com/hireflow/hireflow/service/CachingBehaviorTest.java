package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ApplicationRepository;
import com.hireflow.hireflow.data.repository.CompanyRepository;
import com.hireflow.hireflow.data.repository.JobListingRepository;
import com.hireflow.hireflow.dto.request.CompanyRequest;
import com.hireflow.hireflow.dto.response.CompanyResponse;
import com.hireflow.hireflow.dto.response.JobListingFilterResponse;
import com.hireflow.hireflow.dto.response.JobListingResponse;
import com.hireflow.hireflow.enums.JobType;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.mapper.CompanyMapper;
import com.hireflow.hireflow.mapper.JobListingMapper;
import com.hireflow.hireflow.service.impl.AdminMetricsServiceImpl;
import com.hireflow.hireflow.service.impl.CompanyServiceImpl;
import com.hireflow.hireflow.service.impl.JobListingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.cache.type=simple"
})
@ActiveProfiles("test")
class CachingBehaviorTest {

    @Autowired private CacheManager cacheManager;

    @Autowired private AdminMetricsServiceImpl adminMetricsService;
    @Autowired private JobListingServiceImpl jobListingService;
    @Autowired private CompanyServiceImpl companyService;

    @MockitoBean private ApplicationRepository applicationRepository;
    @MockitoBean private JobListingRepository jobListingRepository;
    @MockitoBean private CompanyRepository companyRepository;
    @MockitoBean private UserService userService;
    @MockitoBean private JobListingMapper jobListingMapper;
    @MockitoBean private CompanyMapper companyMapper;

    private Company company;
    private User admin;

    @BeforeEach
    void resetCaches() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }

        company = new Company();
        company.setId("company-1");
        company.setName("Acme");

        admin = new User("Alex", "Admin", "alex@example.com", "password", Role.ADMIN, true);
        admin.setId("admin-1");
        admin.setCompany(company);
    }

    // ============================================================
    //  AdminMetricsService — adminMetricsVolume / adminMetricsTth
    // ============================================================

    @Test
    @DisplayName("getApplicationVolume: two identical calls only hit the repository once")
    void volume_cachesByCallerAndJob() {
        when(userService.findUserById(admin.getId())).thenReturn(admin);
        when(applicationRepository.countByStageForCompany(eq("company-1"), isNull()))
                .thenReturn(List.of());

        adminMetricsService.getApplicationVolume(admin, null);
        adminMetricsService.getApplicationVolume(admin, null);

        verify(applicationRepository, times(1))
                .countByStageForCompany("company-1", null);
    }

    @Test
    @DisplayName("getApplicationVolume: different jobListingId is a separate cache entry")
    void volume_differentJobListingIdsAreDistinctEntries() {
        when(userService.findUserById(admin.getId())).thenReturn(admin);
        when(applicationRepository.countByStageForCompany(eq("company-1"), any()))
                .thenReturn(List.of());

        adminMetricsService.getApplicationVolume(admin, "job-a");
        adminMetricsService.getApplicationVolume(admin, "job-b");
        adminMetricsService.getApplicationVolume(admin, "job-a");
        adminMetricsService.getApplicationVolume(admin, "job-b");

        verify(applicationRepository, times(1))
                .countByStageForCompany("company-1", "job-a");
        verify(applicationRepository, times(1))
                .countByStageForCompany("company-1", "job-b");
    }

    @Test
    @DisplayName("getTimeToHire: two identical calls only hit the repository once")
    void tth_cachesByCallerAndJob() {
        when(userService.findUserById(admin.getId())).thenReturn(admin);
        when(applicationRepository.findHiredDurationsForCompany(eq("company-1"), isNull()))
                .thenReturn(List.of());

        adminMetricsService.getTimeToHire(admin, null);
        adminMetricsService.getTimeToHire(admin, null);

        verify(applicationRepository, times(1))
                .findHiredDurationsForCompany("company-1", null);
    }

    // ============================================================
    //  JobListingService — jobListings (findById) / jobListingsOpen
    // ============================================================

    @Test
    @DisplayName("findById: same id served from cache on second call")
    void jobListing_findById_cachesById() {
        JobListing job = new JobListing();
        job.setId("job-1");
        JobListingResponse response = new JobListingResponse();
        response.setId("job-1");

        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobListingMapper.toResponse(any(JobListing.class))).thenReturn(response);

        jobListingService.findById("job-1");
        jobListingService.findById("job-1");

        verify(jobListingRepository, times(1)).findById("job-1");
    }

    @Test
    @DisplayName("findById: different ids are stored as separate cache entries")
    void jobListing_findById_differentIdsAreDistinct() {
        JobListing job1 = new JobListing();
        job1.setId("job-1");
        JobListing job2 = new JobListing();
        job2.setId("job-2");

        when(jobListingRepository.findById("job-1")).thenReturn(Optional.of(job1));
        when(jobListingRepository.findById("job-2")).thenReturn(Optional.of(job2));
        when(jobListingMapper.toResponse(any(JobListing.class))).thenAnswer(inv -> {
            JobListingResponse r = new JobListingResponse();
            r.setId(((JobListing) inv.getArgument(0)).getId());
            return r;
        });

        jobListingService.findById("job-1");
        jobListingService.findById("job-2");
        jobListingService.findById("job-1");
        jobListingService.findById("job-2");

        verify(jobListingRepository, times(1)).findById("job-1");
        verify(jobListingRepository, times(1)).findById("job-2");
    }

    @Test
    @DisplayName("findAllOpen: identical (title,type,pageable) triple served from cache on second call")
    void jobListing_findAllOpen_cachesByArgs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobListing> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(jobListingRepository.findAllOpen(eq("java"), eq(JobType.FULL_TIME), eq(pageable)))
                .thenReturn(emptyPage);
        when(jobListingMapper.toJobListingFilterResponse(any(JobListing.class)))
                .thenReturn(new JobListingFilterResponse());

        jobListingService.findAllOpen("java", JobType.FULL_TIME, pageable);
        jobListingService.findAllOpen("java", JobType.FULL_TIME, pageable);

        verify(jobListingRepository, times(1))
                .findAllOpen("java", JobType.FULL_TIME, pageable);
    }

    @Test
    @DisplayName("findAllOpen: different filter strings live in separate cache entries")
    void jobListing_findAllOpen_differentFiltersAreDistinct() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobListing> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(jobListingRepository.findAllOpen(any(), any(), eq(pageable)))
                .thenReturn(emptyPage);
        when(jobListingMapper.toJobListingFilterResponse(any(JobListing.class)))
                .thenReturn(new JobListingFilterResponse());

        jobListingService.findAllOpen("java", JobType.FULL_TIME, pageable);
        jobListingService.findAllOpen("kotlin", JobType.FULL_TIME, pageable);
        jobListingService.findAllOpen("java", JobType.FULL_TIME, pageable);
        jobListingService.findAllOpen("kotlin", JobType.FULL_TIME, pageable);

        verify(jobListingRepository, times(1))
                .findAllOpen("java", JobType.FULL_TIME, pageable);
        verify(jobListingRepository, times(1))
                .findAllOpen("kotlin", JobType.FULL_TIME, pageable);
    }

    // ============================================================
    //  CompanyService — companies cache (findById + evict on update/delete)
    // ============================================================

    @Test
    @DisplayName("findById: company cached on second read")
    void company_findById_cachesById() {
        when(companyRepository.findById("company-1")).thenReturn(Optional.of(company));
        when(companyMapper.toResponse(any(Company.class)))
                .thenReturn(new CompanyResponse());

        companyService.findById("company-1");
        companyService.findById("company-1");

        verify(companyRepository, times(1)).findById("company-1");
    }

    @Test
    @DisplayName("update: evicts the companies cache entry so next read re-fetches")
    void company_update_evictsCacheEntry() {
        when(companyRepository.findById("company-1")).thenReturn(Optional.of(company));
        when(companyMapper.toResponse(any(Company.class)))
                .thenReturn(new CompanyResponse());

        companyService.findById("company-1");
        companyService.findById("company-1");
        verify(companyRepository, times(1)).findById("company-1");
        assertThat(Objects.requireNonNull(cacheManager.getCache("companies")).get("company-1"))
                .as("cache populated before update")
                .isNotNull();

        // Drive update() through the proxy. Requires admin + owner.
        when(userService.findUserById(admin.getId())).thenReturn(admin);
        when(companyRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        CompanyRequest req = new CompanyRequest();
        req.setName("Acme Updated");
        companyService.update("company-1", req, admin);

        assertThat(Objects.requireNonNull(cacheManager.getCache("companies")).get("company-1"))
                .as("cache cleared after update")
                .isNull();

        clearInvocations(companyRepository);
        companyService.findById("company-1");
        verify(companyRepository, times(1)).findById("company-1");
    }

    @Test
    @DisplayName("delete: evicts the companies cache entry so next read re-fetches")
    void company_delete_evictsCacheEntry() {
        when(companyRepository.findById("company-1")).thenReturn(Optional.of(company));
        when(companyMapper.toResponse(any(Company.class)))
                .thenReturn(new CompanyResponse());

        companyService.findById("company-1");
        assertThat(Objects.requireNonNull(cacheManager.getCache("companies")).get("company-1"))
                .as("cache populated before delete")
                .isNotNull();

        when(userService.findUserById(admin.getId())).thenReturn(admin);
        companyService.delete("company-1", admin);

        assertThat(Objects.requireNonNull(cacheManager.getCache("companies")).get("company-1"))
                .as("cache cleared after delete")
                .isNull();
    }
}
