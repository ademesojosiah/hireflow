package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.ScorecardTemplate;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ScorecardTemplateRepository;
import com.hireflow.hireflow.dto.request.ScorecardCriterionRequest;
import com.hireflow.hireflow.dto.request.ScorecardTemplateRequest;
import com.hireflow.hireflow.dto.response.ScorecardTemplateResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.mapper.ScorecardTemplateMapper;
import com.hireflow.hireflow.service.impl.ScorecardTemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScorecardTemplateServiceImplTest {

    @Mock private ScorecardTemplateRepository scorecardTemplateRepository;
    @Mock private UserService userService;

    private ScorecardTemplateServiceImpl service;

    private Company company;
    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId("company-1");

        admin = new User("Aaron", "Admin", "admin@acme.com", "password", Role.ADMIN, true);
        admin.setId("admin-1");
        admin.setCompany(company);

        manager = new User("Maya", "Manager", "maya@acme.com", "password", Role.HMANAGER, true);
        manager.setId("manager-1");
        manager.setCompany(company);

        service = new ScorecardTemplateServiceImpl(
                scorecardTemplateRepository, new ScorecardTemplateMapper(), userService
        );
    }

    @Test
    @DisplayName("Should let admin create a template with criteria")
    void createTemplate_happyPath() {
        when(userService.findUserById(admin.getId())).thenReturn(admin);
        when(scorecardTemplateRepository.existsByCompanyIdAndNameIgnoreCase(company.getId(), "Engineer"))
                .thenReturn(false);
        when(scorecardTemplateRepository.save(any(ScorecardTemplate.class))).thenAnswer(invocation -> {
            ScorecardTemplate template = invocation.getArgument(0);
            template.setId("template-1");
            return template;
        });

        ScorecardTemplateRequest request = new ScorecardTemplateRequest(
                "Engineer",
                "Standard engineering scorecard",
                fiveCriteria()
        );

        ScorecardTemplateResponse response = service.createTemplate(request, admin);

        assertThat(response.getId()).isEqualTo("template-1");
        assertThat(response.getCriteria()).hasSize(5);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should reject duplicate template names within the same company")
    void createTemplate_duplicate() {
        when(userService.findUserById(admin.getId())).thenReturn(admin);
        when(scorecardTemplateRepository.existsByCompanyIdAndNameIgnoreCase(company.getId(), "Engineer"))
                .thenReturn(true);

        ScorecardTemplateRequest request = new ScorecardTemplateRequest(
                "Engineer", null, fiveCriteria()
        );

        assertThatThrownBy(() -> service.createTemplate(request, admin))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(scorecardTemplateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should block non-admin from creating templates")
    void createTemplate_forbiddenForManager() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);

        ScorecardTemplateRequest request = new ScorecardTemplateRequest(
                "Engineer", null, fiveCriteria()
        );

        assertThatThrownBy(() -> service.createTemplate(request, manager))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("admins");
    }

    @Test
    @DisplayName("Should allow HMANAGER to read templates")
    void findTemplates_readableByManager() {
        when(userService.findUserById(manager.getId())).thenReturn(manager);
        when(scorecardTemplateRepository.findAllByCompanyIdAndActive(
                company.getId(), true, org.springframework.data.domain.PageRequest.of(0, 10)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service.findTemplates(true, manager, org.springframework.data.domain.PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should deactivate a template (soft delete) instead of dropping the row")
    void deactivateTemplate_softDelete() {
        ScorecardTemplate template = new ScorecardTemplate();
        template.setId("template-1");
        template.setCompanyId(company.getId());
        template.setActive(true);

        when(userService.findUserById(admin.getId())).thenReturn(admin);
        when(scorecardTemplateRepository.findByIdAndCompanyId("template-1", company.getId()))
                .thenReturn(java.util.Optional.of(template));

        service.deactivateTemplate("template-1", admin);

        assertThat(template.isActive()).isFalse();
        verify(scorecardTemplateRepository).save(template);
    }

    private List<ScorecardCriterionRequest> fiveCriteria() {
        return List.of(
                new ScorecardCriterionRequest("Technical", "System Design", null, 5, 0),
                new ScorecardCriterionRequest("Technical", "Coding", null, 5, 1),
                new ScorecardCriterionRequest("Communication", "Clarity", null, 5, 2),
                new ScorecardCriterionRequest("Behavioral", "Culture Fit", null, 5, 3),
                new ScorecardCriterionRequest("Technical", "Problem Solving", null, 5, 4)
        );
    }
}
