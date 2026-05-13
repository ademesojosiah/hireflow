package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.data.repository.UserRepository;
import com.hireflow.hireflow.dto.response.UserProfileResponse;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.exception.ResourceNotFoundException;
import com.hireflow.hireflow.mapper.UserMapper;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User findUserById(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public UserProfileResponse getMyProfile(User caller) {
        try {
            if (caller == null) {
                throw new AccessDeniedException("Authentication required");
            }
            User user = userRepository.findById(caller.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return userMapper.toProfileResponse(user);
        } catch (AccessDeniedException | ResourceNotFoundException ex) {
            log.warn(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to retrieve current user profile: {}", ex.getMessage());
            throw new CustomException("Failed to retrieve user profile: Internal Server Error");
        }
    }
}
