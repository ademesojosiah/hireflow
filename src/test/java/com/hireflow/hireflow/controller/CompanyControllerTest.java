package com.hireflow.hireflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.hireflow.company.entity.Company;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.company.repository.CompanyRepository;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.company.dto.request.CompanyRequest;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.hireflow.hireflow.event.producer.NotificationEventProducer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockitoBean private NotificationEventProducer notificationEventProducer;

    private User adminUser;
    private User applicantUser;
    private User hiringManagerUser;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(new User(
                "Alice", "Admin", "alice@example.com", passwordEncoder.encode("password123"), Role.ADMIN, true));
        applicantUser = userRepository.save(new User(
                "Bob", "Applicant", "bob@example.com", passwordEncoder.encode("password123"), Role.APPLICANT, true));
        hiringManagerUser = userRepository.save(new User(
                "Charlie", "Manager", "charlie@example.com", passwordEncoder.encode("password123"), Role.HMANAGER, true));
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
        companyRepository.deleteAll();
    }

    private UserPrincipal principalFor(User user) {
        return new UserPrincipal(user);
    }

    @Test
    @DisplayName("Should create a company, link the admin to it, and return 201")
    void create_success() throws Exception {
        CompanyRequest request = new CompanyRequest("Acme", "Tech", "https://acme.io", null, "1-50");

        mockMvc.perform(post("/api/v1/companies")
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Acme"));

        Company persisted = companyRepository.findByName("Acme").orElseThrow();
        User refreshed = userRepository.findById(adminUser.getId()).orElseThrow();
        assertThat(refreshed.getCompany().getId()).isEqualTo(persisted.getId());
    }

    @Test
    @DisplayName("Should return 403 when applicant attempts to create a company")
    void create_forbiddenForApplicant() throws Exception {
        CompanyRequest request = new CompanyRequest("Acme", "Tech", "https://acme.io", null, "1-50");

        mockMvc.perform(post("/api/v1/companies")
                        .with(user(principalFor(applicantUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(companyRepository.findByName("Acme")).isEmpty();
    }

    @Test
    @DisplayName("Should return 400 when company name is blank")
    void create_blankName() throws Exception {
        CompanyRequest request = new CompanyRequest("   ", "Tech", "https://acme.io", null, "1-50");

        mockMvc.perform(post("/api/v1/companies")
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Name is required")));

        assertThat(companyRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 409 when admin already owns a company and tries to create another")
    void create_adminAlreadyOwnsCompany() throws Exception {
        Company existing = new Company();
        existing.setName("Existing Co");
        existing = companyRepository.save(existing);

        adminUser.setCompany(existing);
        userRepository.save(adminUser);

        CompanyRequest request = new CompanyRequest("Brand New", "Tech", "https://brand.io", null, "1-50");

        mockMvc.perform(post("/api/v1/companies")
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already own a company")));

        assertThat(companyRepository.findByName("Brand New")).isEmpty();
    }

    @Test
    @DisplayName("Should return 409 when creating a company with an already-used name")
    void create_duplicateName() throws Exception {
        Company existing = new Company();
        existing.setName("Acme");
        companyRepository.save(existing);

        CompanyRequest request = new CompanyRequest("Acme", "Tech", null, null, null);

        mockMvc.perform(post("/api/v1/companies")
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should allow the owning admin to update their company")
    void update_success() throws Exception {
        Company company = new Company();
        company.setName("Acme");
        company = companyRepository.save(company);

        adminUser.setCompany(company);
        userRepository.save(adminUser);

        CompanyRequest request = new CompanyRequest("Acme Inc", "Software", null, null, "50-100");

        mockMvc.perform(put("/api/v1/companies/" + company.getId())
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Acme Inc"));
    }

    @Test
    @DisplayName("Should return 409 when updating to another company's name")
    void update_duplicateName() throws Exception {
        Company company = new Company();
        company.setName("Acme");
        company = companyRepository.save(company);
        Company existing = new Company();
        existing.setName("Existing Name");
        companyRepository.save(existing);

        adminUser.setCompany(company);
        userRepository.save(adminUser);

        CompanyRequest request = new CompanyRequest("Existing Name", "Software", null, null, "50-100");

        mockMvc.perform(put("/api/v1/companies/" + company.getId())
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    @DisplayName("Should return 403 when an admin who does not own the company tries to update it")
    void update_forbiddenForNonOwner() throws Exception {
        Company company = new Company();
        company.setName("Acme");
        company = companyRepository.save(company);

        CompanyRequest request = new CompanyRequest("Acme Inc", null, null, null, null);

        mockMvc.perform(put("/api/v1/companies/" + company.getId())
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return company by id for any authenticated user")
    void findById_success() throws Exception {
        Company company = new Company();
        company.setName("Acme");
        company = companyRepository.save(company);

        mockMvc.perform(get("/api/v1/companies/" + company.getId())
                .with(user(principalFor(applicantUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Acme"));
    }

    @Test
    @DisplayName("Should return the company details for the signed-in admin")
    void getMyCompany_admin_success() throws Exception {
        Company company = new Company();
        company.setName("Acme");
        company.setIndustry("Tech");
        company = companyRepository.save(company);

        adminUser.setCompany(company);
        userRepository.save(adminUser);

        mockMvc.perform(get("/api/v1/companies/me")
                .with(user(principalFor(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Acme"))
                .andExpect(jsonPath("$.data.industry").value("Tech"));
    }

    @Test
    @DisplayName("Should return the company details for the signed-in hiring manager")
    void getMyCompany_hiringManager_success() throws Exception {
        Company company = new Company();
        company.setName("Beta Corp");
        company = companyRepository.save(company);

        hiringManagerUser.setCompany(company);
        userRepository.save(hiringManagerUser);

        mockMvc.perform(get("/api/v1/companies/me")
                .with(user(principalFor(hiringManagerUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Beta Corp"));
    }

    @Test
    @DisplayName("Should return 403 when applicant tries to access their company")
    void getMyCompany_applicant_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/companies/me")
                .with(user(principalFor(applicantUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 404 when admin has no company")
    void getMyCompany_noCompany() throws Exception {
        mockMvc.perform(get("/api/v1/companies/me")
                .with(user(principalFor(adminUser))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Company not found for the user")));
    }
}
