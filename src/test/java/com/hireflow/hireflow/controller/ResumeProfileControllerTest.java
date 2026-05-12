package com.hireflow.hireflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ResumeProfileRepository;
import com.hireflow.hireflow.data.repository.SkillRepository;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.dto.request.EducationRequest;
import com.hireflow.hireflow.dto.request.ResumeProfileRequest;
import com.hireflow.hireflow.dto.request.WorkExperienceRequest;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResumeProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private ResumeProfileRepository resumeProfileRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User applicant;
    private User hManager;

    @BeforeEach
    void setUp() {
        applicant = userRepository.save(new User(
                "John", "Doe", "john@example.com",
                passwordEncoder.encode("password123"), Role.APPLICANT, true));
        hManager = userRepository.save(new User(
                "Hannah", "Manager", "hannah@example.com",
                passwordEncoder.encode("password123"), Role.HMANAGER, true));
    }

    @AfterEach
    void cleanUp() {
        resumeProfileRepository.deleteAll();
        skillRepository.deleteAll();
        userRepository.deleteAll();
    }

    private UserPrincipal principalFor(User user) {
        return new UserPrincipal(user);
    }

    private Skill saveSkill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        return skillRepository.save(skill);
    }

    private ResumeProfileRequest sampleRequest(Set<String> skillNames) {
        return new ResumeProfileRequest(
                "+2348012345678",
                "https://linkedin.com/in/johndoe",
                "Backend engineer with 5+ years experience.",
                null,
                null,
                skillNames,
                List.of(
                        new WorkExperienceRequest(
                                "Cognetik Technologies",
                                "Backend Engineer",
                                LocalDate.of(2022, 3, 1),
                                null,
                                "<p>Built scalable REST APIs.</p>"),
                        new WorkExperienceRequest(
                                "Tech Solutions Ltd",
                                "Software Developer",
                                LocalDate.of(2020, 1, 1),
                                LocalDate.of(2022, 2, 1),
                                "<p>Worked on frontend.</p>")
                ),
                List.of(new EducationRequest(
                        "MIT",
                        "B.Sc Computer Science",
                        LocalDate.of(2016, 9, 1),
                        LocalDate.of(2020, 6, 1)))
        );
    }

    @Test
    @DisplayName("Should create a new resume profile on first PUT")
    void upsert_create() throws Exception {
        Skill java = saveSkill("Java");
        Skill spring = saveSkill("Spring Boot");
        ResumeProfileRequest request = sampleRequest(Set.of(java.getName(), spring.getName()));

        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.skills.length()").value(2))
                .andExpect(jsonPath("$.data.workExperiences.length()").value(2))
                .andExpect(jsonPath("$.data.educations.length()").value(1));

        assertThat(resumeProfileRepository.findByUser_Id(applicant.getId())).isPresent();
    }

    @Test
    @DisplayName("Should replace existing profile contents on subsequent PUT")
    void upsert_update() throws Exception {
        Skill java = saveSkill("Java");
        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest(Set.of(java.getName())))))
                .andExpect(status().isOk());

        ResumeProfileRequest replacement = new ResumeProfileRequest(
                "+447000000000", "https://linkedin.com/in/updated", "Updated summary.",
                null, null, Set.of(), List.of(), List.of());

        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replacement)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("Updated summary."))
                .andExpect(jsonPath("$.data.skills.length()").value(0))
                .andExpect(jsonPath("$.data.workExperiences.length()").value(0));

        assertThat(resumeProfileRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Should return 404 when an applicant has not created a profile yet")
    void getMyProfile_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/resume-profiles")
                .with(user(principalFor(applicant))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 403 when a hiring manager tries to create a resume profile")
    void upsert_forbiddenForHManager() throws Exception {
        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(hManager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest(null))))
                .andExpect(status().isForbidden());

        assertThat(resumeProfileRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should create skills if they don't exist")
    void upsert_createSkills() throws Exception {
        Skill java = saveSkill("Java");
        ResumeProfileRequest request = sampleRequest(Set.of(java.getName(), "New Skill"));

        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skills.length()").value(2));

        assertThat(skillRepository.findAll()).hasSize(2);
        assertThat(skillRepository.findByNameIgnoreCase("New Skill")).isPresent();
    }


    @Test
    @DisplayName("Should return 400 when work-experience end date precedes start date")
    void upsert_invalidWorkDates() throws Exception {
        ResumeProfileRequest request = new ResumeProfileRequest(
                null, null, null, null, null, null,
                List.of(new WorkExperienceRequest(
                        "Acme",
                        "Engineer",
                        LocalDate.of(2022, 5, 1),
                        LocalDate.of(2022, 1, 1),
                        "x")),
                List.of());

        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when nested work experience required fields are missing")
    void upsert_invalidNestedWorkExperience() throws Exception {
        ResumeProfileRequest request = new ResumeProfileRequest(
                null, null, null, null, null, null,
                List.of(new WorkExperienceRequest("", "Engineer", null, null, "x")),
                List.of());

        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Company name is required")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Start date is required")));

        assertThat(resumeProfileRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 400 when work-experience start date is in the future")
    void upsert_futureWorkStartDate() throws Exception {
        ResumeProfileRequest request = new ResumeProfileRequest(
                null, null, null, null, null, null,
                List.of(new WorkExperienceRequest(
                        "Acme",
                        "Engineer",
                        LocalDate.now().plusDays(1),
                        null,
                        "x")),
                List.of());

        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot be in the future")));

        assertThat(resumeProfileRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 400 when education end date precedes start date")
    void upsert_invalidEducationDates() throws Exception {
        ResumeProfileRequest request = new ResumeProfileRequest(
                null, null, null, null, null, null,
                List.of(),
                List.of(new EducationRequest(
                        "MIT",
                        "B.Sc Computer Science",
                        LocalDate.of(2020, 9, 1),
                        LocalDate.of(2020, 1, 1))));

        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Education end date")));

        assertThat(resumeProfileRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should let a hiring manager retrieve an applicant's profile by user id")
    void findByUserId_hManager() throws Exception {
        Skill java = saveSkill("Java");
        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest(Set.of(java.getName())))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/resume-profiles/user/" + applicant.getId())
                .with(user(principalFor(hManager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(applicant.getId()))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    @DisplayName("Should return 403 when applicant retrieves another user's profile by id")
    void findByUserId_forbiddenForApplicant() throws Exception {
        mockMvc.perform(get("/api/v1/resume-profiles/user/" + hManager.getId())
                        .with(user(principalFor(applicant))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should delete the applicant's profile and cascade child rows")
    void delete_success() throws Exception {
        mockMvc.perform(put("/api/v1/resume-profiles")
                        .with(user(principalFor(applicant)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest(null))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/resume-profiles")
                .with(user(principalFor(applicant))))
                .andExpect(status().isOk());

        assertThat(resumeProfileRepository.findAll()).isEmpty();
    }
}
