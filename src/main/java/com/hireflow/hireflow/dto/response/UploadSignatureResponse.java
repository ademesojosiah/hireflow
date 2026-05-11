package com.hireflow.hireflow.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadSignatureResponse {

    @JsonProperty("cloudName")
    private String cloudName;

    @JsonProperty("apiKey")
    private String apiKey;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("folder")
    private String folder;

    @JsonProperty("resourceType")
    private String resourceType;

}

