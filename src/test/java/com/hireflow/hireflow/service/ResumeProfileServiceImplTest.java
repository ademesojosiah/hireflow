package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.ResumeProfile;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ResumeProfileRepository;
import com.hireflow.hireflow.dto.request.EducationRequest;
import com.hireflow.hireflow.dto.request.ResumeProfileRequest;
import com.hireflow.hireflow.dto.request.WorkExperienceRequest;
import com.hireflow.hireflow.dto.response.ResumeProfileResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.ResumeProfileMapper;
import com.hireflow.hireflow.service.impl.ResumeProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeProfileServiceImplTest {

    @Mock private ResumeProfileRepository resumeProfileRepository;
    @Mock private UserService userService;
    @Mock private SkillService skillService;
    @Mock private ResumeProfileMapper resumeProfileMapper;
    @InjectMocks private ResumeProfileServiceImpl resumeProfileService;

    private User applicant;
    private User hManager;
    private ResumeProfile profile;
    private ResumeProfileRequest request;

    @BeforeEach
    void setUp() {
        applicant = new User();
        applicant.setId("user-1");
        applicant.setEmail("apply@example.com");
        applicant.setRole(Role.APPLICANT);

        hManager = new User();
        hManager.setId("user-2");
        hManager.setRole(Role.HMANAGER);

        profile = new ResumeProfile();
        profile.setId("profile-1");
        profile.setUser(applicant);

        request = new ResumeProfileRequest(
                "+2348012345678",
                "https://linkedin.com/in/me",
                "Backend engineer.",
                null,
                null,
                null,
                List.of(new WorkExperienceRequest("Acme","Backend Developer", LocalDate.of(2022, 3, 1), null, "<p>Built APIs</p>")),
                List.of(new EducationRequest("MIT", "B.Sc CS", LocalDate.of(2016, 9, 1), LocalDate.of(2020, 6, 1)))
        );
    }

    @Test
    @DisplayName("Should create a new profile when none exists for the applicant")
    void upsert_create() {
        when(userService.findUserById("user-1")).thenReturn(applicant);
        when(resumeProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.empty());
        when(resumeProfileMapper.toEntity(eq(request), eq(applicant), isNull())).thenReturn(profile);
        when(resumeProfileRepository.save(profile)).thenReturn(profile);
        when(resumeProfileMapper.toResponse(profile)).thenReturn(new ResumeProfileResponse());

        resumeProfileService.upsertMyProfile(request, applicant);

        verify(resumeProfileMapper).toEntity(eq(request), eq(applicant), isNull());
        verify(resumeProfileRepository).save(profile);
    }

    @Test
    @DisplayName("Should update the existing profile in place when one exists")
    void upsert_update() {
        when(userService.findUserById("user-1")).thenReturn(applicant);
        when(resumeProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.of(profile));
        when(resumeProfileRepository.save(profile)).thenReturn(profile);
        when(resumeProfileMapper.toResponse(profile)).thenReturn(new ResumeProfileResponse());

        resumeProfileService.upsertMyProfile(request, applicant);

        verify(resumeProfileMapper).applyUpdate(eq(profile), eq(request), isNull());
        verify(resumeProfileMapper, never()).toEntity(any(), any(), any());
    }

    @Test
    @DisplayName("Should resolve and attach skills via SkillService when skillNames are provided")
    void upsert_withSkills() {
        Skill java = new Skill();
        java.setId("skill-1");
        java.setName("Java");
        Skill spring = new Skill();
        spring.setId("skill-2");
        spring.setName("Spring");
        Set<String> names = Set.of("Java", "Spring");
        request.setSkillNames(names);
        List<Skill> resolved = List.of(java, spring);

        when(userService.findUserById("user-1")).thenReturn(applicant);
        when(skillService.findOrCreateByNames(names)).thenReturn(resolved);
        when(resumeProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.empty());
        when(resumeProfileMapper.toEntity(eq(request), eq(applicant), eq(resolved))).thenReturn(profile);
        when(resumeProfileRepository.save(profile)).thenReturn(profile);
        when(resumeProfileMapper.toResponse(profile)).thenReturn(new ResumeProfileResponse());

        resumeProfileService.upsertMyProfile(request, applicant);

        verify(skillService).findOrCreateByNames(names);
        verify(resumeProfileMapper).toEntity(eq(request), eq(applicant), eq(resolved));
    }

    @Test
    @DisplayName("Should reject when a non-applicant tries to manage a profile")
    void upsert_forbiddenForNonApplicant() {
        when(userService.findUserById("user-2")).thenReturn(hManager);

        assertThatThrownBy(() -> resumeProfileService.upsertMyProfile(request, hManager))
                .isInstanceOf(AccessDeniedException.class);
        verify(resumeProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when work-experience end date precedes start date")
    void upsert_invalidWorkDates() {
        when(userService.findUserById("user-1")).thenReturn(applicant);
        request.setWorkExperiences(List.of(
                new WorkExperienceRequest("Acme","Backend Developer", LocalDate.of(2022, 5, 1), LocalDate.of(2022, 1, 1), "x")));

        assertThatThrownBy(() -> resumeProfileService.upsertMyProfile(request, applicant))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("end date must be on or after start date");
        verify(resumeProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when work-experience start date is in the future")
    void upsert_futureWorkStartDate() {
        when(userService.findUserById("user-1")).thenReturn(applicant);
        request.setWorkExperiences(List.of(
                new WorkExperienceRequest("Acme", "Backend Developer", LocalDate.now().plusDays(1), null, "x")));

        assertThatThrownBy(() -> resumeProfileService.upsertMyProfile(request, applicant))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("cannot be in the future");

        verify(resumeProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when education end date precedes start date")
    void upsert_invalidEducationDates() {
        when(userService.findUserById("user-1")).thenReturn(applicant);
        request.setEducations(List.of(
                new EducationRequest("MIT", "B.Sc CS", LocalDate.of(2020, 9, 1), LocalDate.of(2020, 1, 1))));

        assertThatThrownBy(() -> resumeProfileService.upsertMyProfile(request, applicant))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Education end date must be on or after start date");

        verify(resumeProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return the applicant's own profile via /me")
    void getMyProfile_success() {
        when(userService.findUserById("user-1")).thenReturn(applicant);
        when(resumeProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.of(profile));
        when(resumeProfileMapper.toResponse(profile)).thenReturn(new ResumeProfileResponse());

        resumeProfileService.getMyProfile(applicant);

        verify(resumeProfileMapper).toResponse(profile);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when the applicant has no profile")
    void getMyProfile_notFound() {
        when(userService.findUserById("user-1")).thenReturn(applicant);
        when(resumeProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeProfileService.getMyProfile(applicant))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should reject getMyProfile when called by a non-applicant")
    void getMyProfile_forbiddenForNonApplicant() {
        when(userService.findUserById("user-2")).thenReturn(hManager);

        assertThatThrownBy(() -> resumeProfileService.getMyProfile(hManager))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only applicants");
    }

    @Test
    @DisplayName("Should find a resume profile by user id")
    void findByUserId_success() {
        ResumeProfileResponse expected = new ResumeProfileResponse();
        when(resumeProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.of(profile));
        when(resumeProfileMapper.toResponse(profile)).thenReturn(expected);

        ResumeProfileResponse response = resumeProfileService.findByUserId("user-1");

        assertThat(response).isSameAs(expected);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when profile user id is unknown")
    void findByUserId_notFound() {
        when(resumeProfileRepository.findByUser_Id("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeProfileService.findByUserId("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");
    }

    @Test
    @DisplayName("Should return profile entity by user id for application submission")
    void findProfileByUserId_success() {
        when(resumeProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.of(profile));

        ResumeProfile result = resumeProfileService.findProfileByUserId("user-1");

        assertThat(result).isSameAs(profile);
    }

    @Test
    @DisplayName("Should delete the applicant's profile and cascade child rows")
    void delete_success() {
        when(userService.findUserById("user-1")).thenReturn(applicant);
        when(resumeProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.of(profile));

        resumeProfileService.deleteMyProfile(applicant);

        verify(resumeProfileRepository).delete(profile);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting a missing profile")
    void delete_notFound() {
        when(userService.findUserById("user-1")).thenReturn(applicant);
        when(resumeProfileRepository.findByUser_Id("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeProfileService.deleteMyProfile(applicant))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");

        verify(resumeProfileRepository, never()).delete(any());
    }
}
