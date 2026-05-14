package com.hireflow.hireflow.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelInterviewRequest {

    @Size(max = 500, message = "reason must be 500 characters or fewer")
    private String reason;
}
