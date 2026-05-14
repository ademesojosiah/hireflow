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
public class ScheduleInterviewRequest {

    @NotNull(message = "startTime is required")
    @Future(message = "startTime must be in the future")
    private Instant startTime;

    @NotNull(message = "endTime is required")
    @Future(message = "endTime must be in the future")
    private Instant endTime;

    /** IANA timezone, e.g. "America/Los_Angeles". Used by the email template for display. */
    @NotBlank(message = "timezone is required")
    @Size(max = 64)
    private String timezone;

    @NotBlank(message = "interviewerEmail is required")
    @Email(message = "interviewerEmail must be a valid email")
    private String interviewerEmail;

    @Size(max = 1000, message = "notes must be 1000 characters or fewer")
    private String notes;
}
