package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.ResumeProfileRequest;
import com.hireflow.hireflow.dto.response.ResumeProfileResponse;

public interface ResumeProfileService {

    ResumeProfileResponse upsertMyProfile(ResumeProfileRequest request, User user);

    ResumeProfileResponse getMyProfile(User user);

    ResumeProfileResponse findByUserId(String userId);

    void deleteMyProfile(User user);

    ResumeProfileResponse updateResumePdfUrl(String pdfUrl, User user);
}
