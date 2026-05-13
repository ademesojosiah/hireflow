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
public class StaffResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Instant createdAt;
}