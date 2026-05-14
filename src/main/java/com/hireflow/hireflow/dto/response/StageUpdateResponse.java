package com.hireflow.hireflow.dto.response;

import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StageUpdateResponse {

    private String id;
    private ApplicationStage previousStage;
    private ApplicationStage currentStage;
    private String reason;
    private String actor;
    private String actorId;
    private String actorEmail;
    private Role actorRole;
    private Instant createdAt;
}
