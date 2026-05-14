package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hireflow.hireflow.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = true)
    private Company company;

    @Column
    private String otp;

    @Column
    private Instant otpExpiry;

    @Column(nullable = false)
    private boolean verified = false;

    public User(String firstName, String lastName, String email, String password, Role role, boolean verified) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.verified = verified;
    }
}
