package com.hireflow.hireflow.security.service;

import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPrincipalService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userService.findByEmail(email)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }


}
