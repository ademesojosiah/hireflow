package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.ScorecardTemplate;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ScorecardTemplateRepository;
import com.hireflow.hireflow.dto.request.ScorecardCriterionRequest;
import com.hireflow.hireflow.dto.request.ScorecardTemplateRequest;
import com.hireflow.hireflow.dto.response.ScorecardTemplateResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.ScorecardTemplateMapper;
import com.hireflow.hireflow.service.ScorecardTemplateService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScorecardTemplateServiceImpl implements ScorecardTemplateService {

    private final ScorecardTemplateRepository scorecardTemplateRepository;
    private final ScorecardTemplateMapper scorecardTemplateMapper;
    private final UserService userService;

    @Override
    @Transactional
    public ScorecardTemplateResponse createTemplate(ScorecardTemplateRequest request, User admin) {
        try {
            User actor = requireAdmin(admin);
            String companyId = actor.getCompany().getId();
            validateFiveCriteria(request);

            if (scorecardTemplateRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.getName())) {
                throw new DuplicateResourceException("A scorecard template with this name already exists");
            }

            ScorecardTemplate template = new ScorecardTemplate();
            template.setName(request.getName());
            template.setDescription(request.getDescription());
            template.setCompanyId(companyId);
            template.setActive(true);
            for (ScorecardCriterionRequest criterionRequest : request.getCriteria()) {
                template.addCriterion(scorecardTemplateMapper.toCriterion(criterionRequest));
            }

            return scorecardTemplateMapper.toResponse(scorecardTemplateRepository.save(template));
        } catch (AccessDeniedException | DuplicateResourceException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Scorecard template create failed: {}", ex.getMessage());
            throw new CustomException("Scorecard template create failed: Internal Server Error");
        }
    }

    @Override
    @Transactional
    public ScorecardTemplateResponse updateTemplate(String templateId, ScorecardTemplateRequest request, User admin) {
        try {
            User actor = requireAdmin(admin);
            ScorecardTemplate template = scorecardTemplateRepository
                    .findByIdAndCompanyId(templateId, actor.getCompany().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Scorecard template not found"));

            validateFiveCriteria(request);
            applyUpdate(template, request);
            return scorecardTemplateMapper.toResponse(scorecardTemplateRepository.save(template));
        } catch (AccessDeniedException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Scorecard template update failed: {}", ex.getMessage());
            throw new CustomException("Scorecard template update failed: Internal Server Error");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ScorecardTemplateResponse findTemplate(String templateId, User user) {
        User actor = requireCompanyManager(user);
        ScorecardTemplate template = scorecardTemplateRepository
                .findByIdAndCompanyId(templateId, actor.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scorecard template not found"));
        return scorecardTemplateMapper.toResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScorecardTemplateResponse> findTemplates(boolean activeOnly, User user, Pageable pageable) {
        User actor = requireCompanyManager(user);
        Page<ScorecardTemplate> templates = activeOnly
                ? scorecardTemplateRepository.findAllByCompanyIdAndActive(actor.getCompany().getId(), true, pageable)
                : scorecardTemplateRepository.findAllByCompanyId(actor.getCompany().getId(), pageable);
        return templates.map(scorecardTemplateMapper::toResponse);
    }

    @Override
    @Transactional
    public void deactivateTemplate(String templateId, User admin) {
        User actor = requireAdmin(admin);
        ScorecardTemplate template = scorecardTemplateRepository
                .findByIdAndCompanyId(templateId, actor.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scorecard template not found"));
        template.setActive(false);
        scorecardTemplateRepository.save(template);
    }

    @Override
    @Transactional(readOnly = true)
    public ScorecardTemplate findTemplateEntityForCompany(String templateId, String companyId) {
        return scorecardTemplateRepository.findByIdAndCompanyId(templateId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Scorecard template not found"));
    }

    /**
     * Manual partial update — never use ModelMapper for update/merge ops (BACKEND_RULES 8.1).
     * For criteria the API contract is "send the full new list" so we clear + re-attach to
     * trigger orphan removal and preserve immutable history of already-submitted scorecards
     * (those snapshot the criteria they used).
     */
    private void applyUpdate(ScorecardTemplate template, ScorecardTemplateRequest request) {
        if (request.getName() != null) template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getCriteria() != null) {
            template.getCriteria().clear();
            for (ScorecardCriterionRequest criterionRequest : request.getCriteria()) {
                template.addCriterion(scorecardTemplateMapper.toCriterion(criterionRequest));
            }
        }
    }

    private void validateFiveCriteria(ScorecardTemplateRequest request) {
        if (request.getCriteria() == null || request.getCriteria().size() != 5) {
            throw new CustomException("A scorecard template must define exactly 5 criteria");
        }
    }

    private User requireAdmin(User user) {
        User refreshed = requireAuthenticated(user);
        if (refreshed.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can manage scorecard templates");
        }
        if (refreshed.getCompany() == null) {
            throw new AccessDeniedException("Admin must belong to a company");
        }
        return refreshed;
    }

    private User requireCompanyManager(User user) {
        User refreshed = requireAuthenticated(user);
        if (refreshed.getRole() != Role.ADMIN && refreshed.getRole() != Role.HMANAGER) {
            throw new AccessDeniedException("Only admins and hiring managers can read scorecard templates");
        }
        if (refreshed.getCompany() == null) {
            throw new AccessDeniedException("User must belong to a company");
        }
        return refreshed;
    }

    private User requireAuthenticated(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        User refreshed = userService.findUserById(user.getId());
        if (refreshed == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return refreshed;
    }
}
