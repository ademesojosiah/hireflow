package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.response.UserProfileResponse;

import java.util.Optional;

public interface UserService {

    Optional<User> findByEmail(String email);

    User findUserById(String userId);


    boolean existsByEmail(String email);

    User save(User user);

    UserProfileResponse getMyProfile(User caller);
}
