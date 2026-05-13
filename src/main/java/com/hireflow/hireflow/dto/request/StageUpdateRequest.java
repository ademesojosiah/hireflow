package com.hireflow.hireflow.dto.request;

import com.hireflow.hireflow.enums.ApplicationStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StageUpdateRequest {

    @NotNull(message = "targetStage is required")
    private ApplicationStage targetStage;

    @Size(max = 500, message = "reason must be 500 characters or fewer")
    private String reason;
}
