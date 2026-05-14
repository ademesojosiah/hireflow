package com.hireflow.hireflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleInterviewRequest {

    @NotNull
    @Future
    private Instant startTime;

    @NotNull
    @Future
    private Instant endTime;

    @NotBlank
    @Size(max = 64)
    private String timezone;

    @Email
    private String interviewerEmail;

    @Size(max = 1000)
    private String notes;
}
