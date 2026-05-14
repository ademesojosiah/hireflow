package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.response.ApplicationVolumeResponse;
import com.hireflow.hireflow.dto.response.TimeToHireResponse;

public interface AdminMetricsService {
    ApplicationVolumeResponse getApplicationVolume(User caller, String jobListingId);
    TimeToHireResponse getTimeToHire(User caller, String jobListingId);
}
