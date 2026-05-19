package com.hireflow.hireflow.company.service.impl;

import com.hireflow.hireflow.company.dto.request.CompanyRequest;
import com.hireflow.hireflow.company.dto.response.CompanyResponse;
import com.hireflow.hireflow.company.entity.Company;
import com.hireflow.hireflow.company.mapper.CompanyMapper;
import com.hireflow.hireflow.company.repository.CompanyRepository;
import com.hireflow.hireflow.company.service.CompanyService;
import com.hireflow.hireflow.config.RedisCacheConfig;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.event.events.EmailNotificationEvent;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.DuplicateResourceException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final CompanyMapper companyMapper;

    @Override
    @Transactional
    public CompanyResponse create(CompanyRequest request, User user) {
        try {
            if (user == null) {
                throw new AccessDeniedException("Authentication required");
            }
            user = userService.findUserById(user.getId());
            requireAdmin(user);

            if (user.getCompany() != null) {
                throw new DuplicateResourceException("You already own a company; an admin can only create one company");
            }

            String name = request.getName().trim();
            request.setName(name);

            if (companyRepository.existsByNameIgnoreCase(name)) {
                throw new DuplicateResourceException("A company with this name already exists");
            }

            Company saved = companyRepository.save(companyMapper.toEntity(request));
            user.setCompany(saved);
            userService.save(user);

            String email = user.getEmail();
            String firstName = user.getFirstName();
            String companyName = saved.getName();
            applicationEventPublisher.publishEvent(new EmailNotificationEvent(
                    EmailNotificationEvent.COMPANY_WELCOME,
                    email,
                    null,
                    firstName,
                    companyName
            ));

            return companyMapper.toResponse(saved);
        } catch (AccessDeniedException | DuplicateResourceException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Company creation failed: {}", ex.getMessage());
            throw new CustomException("Company creation failed: Internal Server Error");
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.COMPANIES, key = "#id")
    public CompanyResponse update(String id, CompanyRequest request, User user) {
        try {
            if (user == null) {
                throw new AccessDeniedException("Authentication required");
            }
            user = userService.findUserById(user.getId());
            Company company = companyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
            requireOwner(user, company);

            String name = request.getName().trim();
            request.setName(name);
            if (!company.getName().equalsIgnoreCase(name) && companyRepository.existsByNameIgnoreCase(name)) {
                throw new DuplicateResourceException("A company with this name already exists");
            }

            companyMapper.applyUpdate(company, request);
            return companyMapper.toResponse(companyRepository.save(company));
        } catch (AccessDeniedException | ResourceNotFoundException | DuplicateResourceException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Company update failed: {}", ex.getMessage());
            throw new CustomException("Company update failed: Internal Server Error");
        }
    }

    @Override
    @Cacheable(value = RedisCacheConfig.COMPANIES, key = "#id")
    public CompanyResponse findById(String id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return companyMapper.toResponse(company);
    }

    @Override
    public Page<CompanyResponse> findAll(Pageable pageable) {
        return companyRepository.findAll(pageable).map(companyMapper::toResponse);
    }

    @Override
    public Company findCompanyById(String id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.COMPANIES, key = "#id")
    public void delete(String id, User user) {
        try {
            if (user == null) {
                throw new AccessDeniedException("Authentication required");
            }
            user = userService.findUserById(user.getId());
            Company company = companyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
            requireOwner(user, company);
            companyRepository.delete(company);
        } catch (AccessDeniedException | ResourceNotFoundException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Company deletion failed: {}", ex.getMessage());
            throw new CustomException("Company deletion failed: Internal Server Error");
        }
    }

    @Override
    public CompanyResponse getMyCompany(User user) {
        try {
            requireAdminOrHiringManager(user);

            if (user.getCompany() == null) {
                throw new ResourceNotFoundException("Company not found for the user");
            }

            Company company = findCompanyById(user.getCompany().getId());
            return companyMapper.toResponse(company);
        } catch (AccessDeniedException | ResourceNotFoundException ex) {
            log.error(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to retrieve company for user: {}", ex.getMessage());
            throw new CustomException("Failed to retrieve company: Internal Server Error");
        }
    }

    private void requireAdmin(User user) {
        if (user == null || user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
    }

    private void requireOwner(User user, Company company) {
        requireAdmin(user);
        if (user.getCompany() == null || !user.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Only the admin who owns this company can perform this action");
        }
    }

    private void requireAdminOrHiringManager(User user) {
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.HMANAGER)) {
            throw new AccessDeniedException("Only admins or hiring managers can perform this action");
        }
    }
}
