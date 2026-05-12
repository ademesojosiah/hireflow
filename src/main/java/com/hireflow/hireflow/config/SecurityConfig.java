package com.hireflow.hireflow.config;

import com.hireflow.hireflow.security.filter.JwtAuthenticationFilter;
import com.hireflow.hireflow.security.service.UserPrincipalService;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserPrincipalService userPrincipalService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/companies/**").authenticated()
                        .requestMatchers("/api/v1/companies/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").authenticated()
                        .requestMatchers("/api/v1/jobs/**").hasAnyRole("ADMIN", "HMANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/v1/skills/**").authenticated()
                        .requestMatchers("/api/v1/skills/**").hasAnyRole("ADMIN", "HMANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/v1/resume-profiles/user/**").hasAnyRole("ADMIN", "HMANAGER")
                        .requestMatchers("/api/v1/resume-profiles/**").hasRole("APPLICANT")

                        .requestMatchers("/api/v1/uploads/**").hasRole("APPLICANT")

                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/jobs/**").hasRole("APPLICANT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/applications/me").hasRole("APPLICANT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/applications/jobs/**").hasAnyRole("ADMIN", "HMANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/applications/**").hasRole("APPLICANT")

                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userPrincipalService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowCredentials(true);

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://yourfrontend.com"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
