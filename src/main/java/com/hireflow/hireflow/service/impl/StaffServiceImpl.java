package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.dto.response.StaffResponse;
import com.hireflow.hireflow.enums.Role;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.UserMapper;
import com.hireflow.hireflow.service.StaffService;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private static final List<Role> STAFF_ROLES = List.of(Role.ADMIN, Role.HMANAGER);

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserMapper userMapper;

    @Override
    public Page<StaffResponse> findStaff(User caller, Pageable pageable) {
        try {
            User admin = requireAdminCaller(caller);
            String companyId = requireCompanyId(admin);
            return userRepository
                    .findAllByCompanyIdAndRoleIn(companyId, STAFF_ROLES, pageable)
                    .map(userMapper::toStaffResponse);
        } catch (AccessDeniedException | ResourceNotFoundException ex) {
            log.warn(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to retrieve staff list: {}", ex.getMessage());
            throw new CustomException("Failed to retrieve staff: Internal Server Error");
        }
    }

    @Override
    @Transactional
    public void deleteStaff(String staffId, User caller) {
        try {
            User admin = requireAdminCaller(caller);
            String companyId = requireCompanyId(admin);

            if (admin.getId().equals(staffId)) {
                throw new AccessDeniedException("You cannot delete your own account");
            }

            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

            if (!STAFF_ROLES.contains(staff.getRole())) {
                throw new AccessDeniedException("Only ADMIN or HMANAGER accounts can be removed via this endpoint");
            }

            Company staffCompany = staff.getCompany();
            if (staffCompany == null || !companyId.equals(staffCompany.getId())) {
                throw new AccessDeniedException("You can only manage staff in your own company");
            }

            userRepository.delete(staff);
            log.info("Admin {} removed staff {} (role={}) from company {}",
                    admin.getId(), staff.getId(), staff.getRole(), companyId);
        } catch (AccessDeniedException | ResourceNotFoundException ex) {
            log.warn(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Staff deletion failed: {}", ex.getMessage());
            throw new CustomException("Staff deletion failed: Internal Server Error");
        }
    }

    private User requireAdminCaller(User caller) {
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }
        User admin = userService.findUserById(caller.getId());
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
        return admin;
    }

    private String requireCompanyId(User admin) {
        Company company = admin.getCompany();
        if (company == null) {
            throw new ResourceNotFoundException("Company not found for the user");
        }
        return company.getId();
    }
}