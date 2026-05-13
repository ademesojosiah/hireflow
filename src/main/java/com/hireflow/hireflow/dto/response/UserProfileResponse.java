package com.hireflow.hireflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean verified;
    private String companyId;
    private String companyName;
    private Instant createdAt;
}