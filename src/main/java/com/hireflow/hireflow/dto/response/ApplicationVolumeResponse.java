package com.hireflow.hireflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hireflow.hireflow.enums.ApplicationStage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationVolumeResponse {
    private Map<ApplicationStage, Long> volumeByStage;
    private long total;
    private String jobListingId;
}
