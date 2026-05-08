package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.response.AuthResponse;
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
}
