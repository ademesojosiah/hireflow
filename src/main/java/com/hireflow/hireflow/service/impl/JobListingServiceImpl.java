package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.JobListing;
import com.hireflow.hireflow.data.model.Skill;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.JobListingRepository;
import com.hireflow.hireflow.dto.request.JobListingRequest;
import com.hireflow.hireflow.dto.response.JobListingFilterResponse;
import com.hireflow.hireflow.dto.response.JobListingResponse;
import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.JobType;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.JobListingMapper;
import com.hireflow.hireflow.service.CompanyService;
import com.hireflow.hireflow.service.JobListingService;
import com.hireflow.hireflow.service.SkillService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobListingServiceImpl implements JobListingService {

    private final JobListingRepository jobListingRepository;
    private final SkillService skillService;
    private final CompanyService companyService;
    private final UserService userService;
    private final JobListingMapper jobListingMapper;

    @Override
    @Transactional
    public JobListingResponse create(JobListingRequest request, User user) {
        try {
            Company company = requireCompanyManager(user);
            validateThresholds(request);
            List<Skill> skills = resolveSkills(request.getSkillIds());
            JobListing job = jobListingMapper.toEntity(request, company, skills);
            return jobListingMapper.toResponse(jobListingRepository.save(job));
        } catch (AccessDeniedException | CustomException | ResourceNotFoundException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Job listing creation failed: {}", ex.getMessage());
            throw new CustomException("Job listing creation failed: Internal Server Error");
        }
    }

    @Override
    @Transactional
    public JobListingResponse update(String id, JobListingRequest request, User user) {
        try {
            JobListing job = jobListingRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));
            requireJobOwner(user, job);
            validateThresholds(request);
            List<Skill> skills = request.getSkillIds() == null ? null : resolveSkills(request.getSkillIds());
            jobListingMapper.applyUpdate(job, request, skills);
            return jobListingMapper.toResponse(jobListingRepository.save(job));
        } catch (AccessDeniedException | ResourceNotFoundException | CustomException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Job listing update failed: {}", ex.getMessage());
            throw new CustomException("Job listing update failed: Internal Server Error");
        }
    }

    @Override
    public JobListingResponse findById(String id) {
        JobListing job = jobListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));
        return jobListingMapper.toResponse(job);
    }

    @Override
    public JobListing findJobListingById(String id) {
        return jobListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));
    }

    @Override
    public Page<JobListingResponse> findByCompany(String companyId, JobStatus status, Pageable pageable) {
        Page<JobListing> page = (status == null)
                ? jobListingRepository.findAllByCompany_Id(companyId, pageable)
                : jobListingRepository.findAllByCompany_IdAndStatus(companyId, status, pageable);
        return page.map(jobListingMapper::toResponse);
    }

    @Override
    public Page<JobListingResponse> findByCompany(JobStatus status,  User user, Pageable pageable) {
        Company company = requireCompanyManager(user);
        String companyId = company.getId();
        Page<JobListing> page = (status == null)
                ? jobListingRepository.findAllByCompany_Id(companyId, pageable)
                : jobListingRepository.findAllByCompany_IdAndStatus(companyId, status, pageable);
        return page.map(jobListingMapper::toResponse);
    }
    @Override
    public Page<JobListingFilterResponse> findAllOpen(String title, JobType type, Pageable pageable) {
        return jobListingRepository.findAllOpen(normalizeFilter(title), type, pageable)
                .map(jobListingMapper::toJobListingFilterResponse);
    }

    @Override
    @Transactional
    public void delete(String id, User user) {
        try {
            JobListing job = jobListingRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));
            requireJobOwner(user, job);
            jobListingRepository.delete(job);
        } catch (AccessDeniedException | ResourceNotFoundException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Job listing deletion failed: {}", ex.getMessage());
            throw new CustomException("Job listing deletion failed: Internal Server Error");
        }
    }

    private Company requireCompanyManager(User user) {
        user = userService.findUserById(user.getId());
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.HMANAGER)) {
            throw new AccessDeniedException("Only admins and hiring managers can manage job listings");
        }
        if (user.getCompany() == null) {
            throw new AccessDeniedException("You must belong to a company to manage job listings");
        }
        return companyService.findCompanyById(user.getCompany().getId());
    }

    private void requireJobOwner(User user, JobListing job) {
        Company userCompany = requireCompanyManager(user);
        if (!userCompany.getId().equals(job.getCompany().getId())) {
            throw new AccessDeniedException("You can only manage job listings for your own company");
        }
    }

    private void validateThresholds(JobListingRequest request) {
        if (request.getAutoRejectThreshold() >= request.getAutoPassThreshold()) {
            throw new CustomException("Auto-reject threshold must be less than auto-pass threshold");
        }
    }

    private List<Skill> resolveSkills(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return skillService.findAllByIds(ids);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
