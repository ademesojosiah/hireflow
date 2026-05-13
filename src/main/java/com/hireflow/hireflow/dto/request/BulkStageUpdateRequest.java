package com.hireflow.hireflow.dto.request;

import com.hireflow.hireflow.enums.ApplicationStage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkStageUpdateRequest {

    @NotEmpty(message = "applicationIds is required")
    @Size(max = 200, message = "Up to 200 applications can be updated in a single bulk request")
    private List<String> applicationIds;

    @NotNull(message = "targetStage is required")
    private ApplicationStage targetStage;

    @Size(max = 500, message = "reason must be 500 characters or fewer")
    private String reason;
}
