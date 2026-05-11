package com.hireflow.hireflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CloudinarySignatureResponse {

    private String cloudName;
    private String apiKey;
    private long timestamp;
    private String folder;
    private String resourceType;
    private String signature;
}
