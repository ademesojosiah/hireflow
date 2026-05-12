package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.ResumeProfile;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.ResumeProfileRepository;
import com.hireflow.hireflow.dto.request.ResumeProfileRequest;
import com.hireflow.hireflow.dto.response.ResumeProfileResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.ResumeProfileMapper;
import com.hireflow.hireflow.service.ResumeProfileService;
import com.hireflow.hireflow.service.SkillService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeProfileServiceImpl implements ResumeProfileService {

    private final ResumeProfileRepository resumeProfileRepository;
    private final UserService userService;
    private final SkillService skillService;
    private final ResumeProfileMapper resumeProfileMapper;

    @Override
    @Transactional
    public ResumeProfileResponse upsertMyProfile(ResumeProfileRequest request, User user) {
        try {
            User refreshed = requireApplicant(user);
            validateDates(request);

            List<Skill> skills = request.getSkillNames() == null ? null
                    : skillService.findOrCreateByNames(request.getSkillNames());

            Optional<ResumeProfile> existing = resumeProfileRepository.findByUser_Id(refreshed.getId());

            ResumeProfile saved;
            if (existing.isPresent()) {
                ResumeProfile profile = existing.get();
                profile.getSkills().clear();
                profile.getWorkExperiences().clear();
                profile.getEducations().clear();
                resumeProfileRepository.saveAndFlush(profile);
                resumeProfileMapper.applyUpdate(profile, request, skills);
                saved = resumeProfileRepository.save(profile);
            } else {
                ResumeProfile profile = resumeProfileMapper.toEntity(request, refreshed, skills);
                saved = resumeProfileRepository.save(profile);
            }

            return resumeProfileMapper.toResponse(saved);
        } catch (AccessDeniedException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Resume profile upsert failed: {}", ex.getMessage());
            throw new CustomException("Resume profile save failed: Internal Server Error");
        }
    }

    @Override
    public ResumeProfileResponse getMyProfile(User user) {
        try {
            User refreshed = requireApplicant(user);
            ResumeProfile profile = resumeProfileRepository.findByUser_Id(refreshed.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume profile not found"));
            return resumeProfileMapper.toResponse(profile);
        } catch (AccessDeniedException | ResourceNotFoundException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to retrieve resume profile: {}", ex.getMessage());
            throw new CustomException("Failed to retrieve resume profile: Internal Server Error");
        }
    }

    @Override
    public ResumeProfileResponse findByUserId(String userId) {
        ResumeProfile profile = resumeProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume profile not found"));
        return resumeProfileMapper.toResponse(profile);
    }

    @Override
    public ResumeProfile findProfileByUserId(String userId) {
        return resumeProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume profile not found"));
    }

    @Override
    @Transactional
    public void deleteMyProfile(User user) {
        try {
            User refreshed = requireApplicant(user);
            ResumeProfile profile = resumeProfileRepository.findByUser_Id(refreshed.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume profile not found"));
            resumeProfileRepository.delete(profile);
        } catch (AccessDeniedException | ResourceNotFoundException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Resume profile deletion failed: {}", ex.getMessage());
            throw new CustomException("Resume profile deletion failed: Internal Server Error");
        }
    }


    private User requireApplicant(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        User refreshed = userService.findUserById(user.getId());
        if (refreshed.getRole() != Role.APPLICANT) {
            throw new AccessDeniedException("Only applicants can manage a resume profile");
        }
        return refreshed;
    }

    private void validateDates(ResumeProfileRequest request) {
        if (request.getWorkExperiences() != null) {
            request.getWorkExperiences().forEach(we -> {
                if (we.getEndDate() != null && we.getEndDate().isBefore(we.getStartDate())) {
                    throw new CustomException("Work experience end date must be on or after start date");
                }
                if (we.getStartDate() != null && we.getStartDate().isAfter(LocalDate.now())) {
                    throw new CustomException("Work experience start date cannot be in the future");
                }
            });
        }
        if (request.getEducations() != null) {
            request.getEducations().forEach(ed -> {
                if (ed.getEndDate() != null && ed.getEndDate().isBefore(ed.getStartDate())) {
                    throw new CustomException("Education end date must be on or after start date");
                }
            });
        }
    }
}
