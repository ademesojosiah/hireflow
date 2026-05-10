package com.hireflow.hireflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EducationResponse {

    private String id;
    private String institutionName;
    private String degree;
    private LocalDate startDate;
    private LocalDate endDate;
}
