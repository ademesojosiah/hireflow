package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.response.AuthResponse;
import com.hireflow.hireflow.dto.response.StaffResponse;
import com.hireflow.hireflow.dto.response.UserProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public AuthResponse toAuthResponse(User user, String token) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        return response;
    }

    public UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setVerified(user.isVerified());
        Company company = user.getCompany();
        if (company != null) {
            response.setCompanyId(company.getId());
            response.setCompanyName(company.getName());
        }
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public StaffResponse toStaffResponse(User user) {
        StaffResponse response = new StaffResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
