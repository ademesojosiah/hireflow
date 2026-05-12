package com.hireflow.hireflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JobQuestionRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 5000, message = "Question must not exceed 5000 characters")
    private String question;

    @NotBlank(message = "Answer is required")
    @Size(max = 5000, message = "Answer must not exceed 5000 characters")
    private String answer;
}
