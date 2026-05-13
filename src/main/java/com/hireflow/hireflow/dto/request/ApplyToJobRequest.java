package com.hireflow.hireflow.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplyToJobRequest {

    @Valid
    private List<ApplicationAnswerRequest> answers = new ArrayList<>();
}
