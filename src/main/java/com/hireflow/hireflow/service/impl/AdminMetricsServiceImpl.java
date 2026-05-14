package com.hireflow.hireflow.service.impl;

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
import com.hireflow.hireflow.service.AdminMetricsService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminMetricsServiceImpl implements AdminMetricsService {

    private final ApplicationRepository applicationRepository;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public ApplicationVolumeResponse getApplicationVolume(User caller, String jobListingId) {
        User admin = requireAdminCaller(caller);
        String companyId = requireCompanyId(admin);

        List<StageVolumeProjection> rows = applicationRepository.countByStageForCompany(companyId, jobListingId);

        Map<ApplicationStage, Long> volumeByStage = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : ApplicationStage.values()) {
            volumeByStage.put(stage, 0L);
        }
        long total = 0L;
        for (StageVolumeProjection row : rows) {
            long count = row.getCount() == null ? 0L : row.getCount();
            volumeByStage.put(row.getStage(), count);
            total += count;
        }

        return new ApplicationVolumeResponse(volumeByStage, total, jobListingId);
    }

    @Override
    @Transactional(readOnly = true)
    public TimeToHireResponse getTimeToHire(User caller, String jobListingId) {
        User admin = requireAdminCaller(caller);
        String companyId = requireCompanyId(admin);

        List<TimeToHireProjection> rows = applicationRepository.findHiredDurationsForCompany(companyId, jobListingId);

        List<Long> durationsHours = new ArrayList<>(rows.size());
        for (TimeToHireProjection row : rows) {
            Instant appliedAt = row.getAppliedAt();
            Instant hiredAt = row.getHiredAt();
            if (appliedAt == null || hiredAt == null || !hiredAt.isAfter(appliedAt)) {
                continue;
            }
            durationsHours.add(Duration.between(appliedAt, hiredAt).toHours());
        }

        if (durationsHours.isEmpty()) {
            return new TimeToHireResponse(0, null, null, null, null, null, jobListingId);
        }

        durationsHours.sort(Long::compareTo);
        long sum = 0L;
        for (Long h : durationsHours) {
            sum += h;
        }
        long mean = Math.round((double) sum / durationsHours.size());

        return new TimeToHireResponse(
                durationsHours.size(),
                mean,
                percentile(durationsHours, 50),
                percentile(durationsHours, 95),
                durationsHours.get(0),
                durationsHours.get(durationsHours.size() - 1),
                jobListingId
        );
    }

    private static Long percentile(List<Long> sortedAsc, int percentile) {
        if (sortedAsc.isEmpty()) {
            return null;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedAsc.size()) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= sortedAsc.size()) {
            index = sortedAsc.size() - 1;
        }
        return sortedAsc.get(index);
    }

    private User requireAdminCaller(User caller) {
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }
        User admin = userService.findUserById(caller.getId());
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
        return admin;
    }

    private String requireCompanyId(User admin) {
        Company company = admin.getCompany();
        if (company == null) {
            throw new ResourceNotFoundException("Company not found for the user");
        }
        return company.getId();
    }
}
