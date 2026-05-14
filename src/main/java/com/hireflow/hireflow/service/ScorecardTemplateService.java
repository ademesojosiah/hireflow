package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.ScorecardTemplate;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.ScorecardTemplateRequest;
import com.hireflow.hireflow.dto.response.ScorecardTemplateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScorecardTemplateService {

    ScorecardTemplateResponse createTemplate(ScorecardTemplateRequest request, User admin);

    ScorecardTemplateResponse updateTemplate(String templateId, ScorecardTemplateRequest request, User admin);

    ScorecardTemplateResponse findTemplate(String templateId, User user);

    Page<ScorecardTemplateResponse> findTemplates(boolean activeOnly, User user, Pageable pageable);

    void deactivateTemplate(String templateId, User admin);

    ScorecardTemplate findTemplateEntityForCompany(String templateId, String companyId);
}
