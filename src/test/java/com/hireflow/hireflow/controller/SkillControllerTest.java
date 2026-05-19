package com.hireflow.hireflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.hireflow.skill.entity.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.skill.repository.SkillRepository;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.skill.dto.request.SkillRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkillControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User applicantUser;

    @BeforeEach
    void setUp() {
        skillRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(new User(
                "Alice", "Admin", "skill-admin@example.com", passwordEncoder.encode("password123"), Role.ADMIN, true));
        applicantUser = userRepository.save(new User(
                "Bob", "Applicant", "skill-applicant@example.com", passwordEncoder.encode("password123"), Role.APPLICANT, true));
    }

    @AfterEach
    void cleanUp() {
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

    @Test
    @DisplayName("Should create a skill and return 201")
    void create_success() throws Exception {
        SkillRequest request = new SkillRequest("  Scala  ");

        mockMvc.perform(post("/api/v1/skills")
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Scala"));

        assertThat(skillRepository.existsByNameIgnoreCase("scala")).isTrue();
    }

    @Test
    @DisplayName("Should return 409 when creating a duplicate skill")
    void create_duplicateName() throws Exception {
        saveSkill("Scala");
        SkillRequest request = new SkillRequest("scala");

        mockMvc.perform(post("/api/v1/skills")
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 400 when creating a blank skill")
    void create_blankName() throws Exception {
        SkillRequest request = new SkillRequest("   ");

        mockMvc.perform(post("/api/v1/skills")
                        .with(user(principalFor(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Skill name is required")));

        assertThat(skillRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 403 when applicant attempts to create a skill")
    void create_forbiddenForApplicant() throws Exception {
        SkillRequest request = new SkillRequest("Scala");

        mockMvc.perform(post("/api/v1/skills")
                        .with(user(principalFor(applicantUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(skillRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should search skills by a three-character prefix")
    void search_success() throws Exception {
        saveSkill("ZedAlpha");
        saveSkill("ZedBeta");
        saveSkill("ZenDesk");

        mockMvc.perform(get("/api/v1/skills/search")
                        .param("query", "zed")
                .with(user(principalFor(applicantUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("ZedAlpha"))
                .andExpect(jsonPath("$.data[1].name").value("ZedBeta"));
    }

    @Test
    @DisplayName("Should return 400 when search query has fewer than three characters")
    void search_tooShort() throws Exception {
        mockMvc.perform(get("/api/v1/skills/search")
                        .param("query", "ze")
                .with(user(principalFor(applicantUser))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 400 when search query is missing")
    void search_missingQuery() throws Exception {
        mockMvc.perform(get("/api/v1/skills/search")
                        .with(user(principalFor(applicantUser))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("query is required"));
    }
}
