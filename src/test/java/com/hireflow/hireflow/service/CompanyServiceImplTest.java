package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.CompanyRepository;
import com.hireflow.hireflow.dto.request.CompanyRequest;
import com.hireflow.hireflow.dto.response.CompanyResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.CompanyMapper;
import com.hireflow.hireflow.service.impl.CompanyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserService userService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private CompanyMapper companyMapper;
    @InjectMocks private CompanyServiceImpl companyService;

    private User adminUser;
    private User applicantUser;
    private User hiringManagerUser;
    private Company company;
    private CompanyRequest request;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId("admin-id");
        adminUser.setEmail("admin@example.com");
        adminUser.setFirstName("Alice");
        adminUser.setRole(Role.ADMIN);

        applicantUser = new User();
        applicantUser.setId("applicant-id");
        applicantUser.setEmail("applicant@example.com");
        applicantUser.setRole(Role.APPLICANT);

        hiringManagerUser = new User();
        hiringManagerUser.setId("manager-id");
        hiringManagerUser.setEmail("manager@example.com");
        hiringManagerUser.setRole(Role.HMANAGER);

        company = new Company();
        company.setId("company-id");
        company.setName("Acme");

        request = new CompanyRequest("Acme", "Tech", "https://acme.io", null, "1-50");
    }

    @Test
    @DisplayName("Should create company, link to admin, and send welcome email")
    void create_success() {

        when(userService.findUserById(adminUser.getId())).thenReturn(adminUser);
        when(companyRepository.existsByNameIgnoreCase("Acme")).thenReturn(false);
        when(companyMapper.toEntity(request)).thenReturn(company);
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toResponse(company)).thenReturn(new CompanyResponse("company-id", "Acme", "Tech", null, null, null));

        CompanyResponse response = companyService.create(request, adminUser);

        assertThat(response.getName()).isEqualTo("Acme");
        assertThat(adminUser.getCompany()).isEqualTo(company);
        verify(userService).save(adminUser);
        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof EmailNotificationEvent emailEvent
                        && EmailNotificationEvent.COMPANY_WELCOME.equals(emailEvent.getType())
                        && "admin@example.com".equals(emailEvent.getTo())
                        && "Alice".equals(emailEvent.getFirstName())
                        && "Acme".equals(emailEvent.getCompanyName())
        ));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-admin tries to create company")
    void create_nonAdmin() {
        assertThatThrownBy(() -> companyService.create(request, applicantUser))
                .isInstanceOf(AccessDeniedException.class);

        verify(companyRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when company name already exists")
    void create_duplicateName() {

        when(userService.findUserById(adminUser.getId())).thenReturn(adminUser);
        when(companyRepository.existsByNameIgnoreCase("Acme")).thenReturn(true);

        assertThatThrownBy(() -> companyService.create(request, adminUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when admin already owns a company")
    void create_adminAlreadyOwnsCompany() {
        Company existing = new Company();
        existing.setId("existing-company-id");
        existing.setName("Existing Co");
        adminUser.setCompany(existing);
        when(userService.findUserById(adminUser.getId())).thenReturn(adminUser);

        assertThatThrownBy(() -> companyService.create(request, adminUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already own a company");

        verify(companyRepository, never()).existsByNameIgnoreCase(any());
        verify(companyRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Should update company when called by its admin owner")
    void update_success() {
        adminUser.setCompany(company);

        when(userService.findUserById(adminUser.getId())).thenReturn(adminUser);
        when(companyRepository.findById("company-id")).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toResponse(company)).thenReturn(new CompanyResponse("company-id", "Acme", "Tech", null, null, null));

        CompanyResponse response = companyService.update("company-id", request, adminUser);

        assertThat(response.getId()).isEqualTo("company-id");
        verify(companyMapper).applyUpdate(company, request);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when admin does not own the company")
    void update_notOwner() {
        Company otherCompany = new Company();
        otherCompany.setId("other-id");
        adminUser.setCompany(otherCompany);
        when(companyRepository.findById("company-id")).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> companyService.update("company-id", request, adminUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating a non-existent company")
    void update_notFound() {
        when(companyRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.update("missing", request, adminUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should delete company when called by its admin owner")
    void delete_success() {
        adminUser.setCompany(company);
        when(userService.findUserById(adminUser.getId())).thenReturn(adminUser);
        when(companyRepository.findById("company-id")).thenReturn(Optional.of(company));

        companyService.delete("company-id", adminUser);

        verify(companyRepository).delete(company);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when non-owner attempts delete")
    void delete_notOwner() {
        when(companyRepository.findById("company-id")).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> companyService.delete("company-id", adminUser))
                .isInstanceOf(AccessDeniedException.class);

        verify(companyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should return company for admin user")
    void getMyCompany_admin_success() {
        adminUser.setCompany(company);
        CompanyResponse expectedResponse = new CompanyResponse("company-id", "Acme", null, null, null, null);
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        CompanyResponse response = companyService.getMyCompany(adminUser);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("Should return company for hiring manager user")
    void getMyCompany_hiringManager_success() {
        hiringManagerUser.setCompany(company);
        CompanyResponse expectedResponse = new CompanyResponse("company-id", "Acme", null, null, null, null);
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        CompanyResponse response = companyService.getMyCompany(hiringManagerUser);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when applicant tries to get company")
    void getMyCompany_applicant_accessDenied() {
        assertThatThrownBy(() -> companyService.getMyCompany(applicantUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only admins or hiring managers");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when admin has no company")
    void getMyCompany_noCompany() {
        assertThatThrownBy(() -> companyService.getMyCompany(adminUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Company not found for the user");
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user is null")
    void getMyCompany_nullUser() {
        assertThatThrownBy(() -> companyService.getMyCompany(null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
