package com.hireflow.hireflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkStageUpdateResponse {

    private int requested;
    private int succeeded;
    private int failed;
    private List<String> updatedApplicationIds;
    private List<BulkStageUpdateFailure> failures;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkStageUpdateFailure {
        private String applicationId;
        private String reason;
    }
}
