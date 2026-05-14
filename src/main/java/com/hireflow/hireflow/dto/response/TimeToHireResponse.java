package com.hireflow.hireflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeToHireResponse {
    private int sampleSize;
    private Long meanHours;
    private Long medianHours;
    private Long p95Hours;
    private Long minHours;
    private Long maxHours;
    private String jobListingId;
}
