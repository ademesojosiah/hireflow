package com.hireflow.hireflow.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.hireflow.hireflow.security.UserPrincipal;
import com.hireflow.hireflow.security.UserPrincipalAuthenticationToken;
import com.hireflow.hireflow.security.service.UserPrincipalService;
import com.hireflow.hireflow.security.util.JwtUtil;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserPrincipalService userPrincipalService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            extractToken(request)
                    .filter(jwtUtil::isTokenValid)
                    .map(jwtUtil::extractSubject)
                    .map(userPrincipalService::loadUserByUsername)
                    .map(user -> new UserPrincipalAuthenticationToken((UserPrincipal) user))
                    .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));

            filterChain.doFilter(request, response);

    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }
}
