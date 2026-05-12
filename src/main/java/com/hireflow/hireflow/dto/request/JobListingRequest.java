package com.hireflow.hireflow.dto.request;

import com.hireflow.hireflow.enums.JobStatus;
import com.hireflow.hireflow.enums.JobType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JobListingRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @NotNull(message = "Job type is required")
    private JobType type;

    @Size(max = 200)
    private String location;

    @NotBlank(message = "Summary is required")
    @Size(max = 5000)
    private String summary;

    @NotBlank(message = "Responsibilities are required")
    private String responsibilities;

    @NotBlank(message = "Required qualifications are required")
    private String requiredQualifications;

    private String preferredQualifications;

    private JobStatus status;

    @NotNull(message = "Auto-reject threshold is required")
    @Min(value = 0, message = "Auto-reject threshold must be between 0 and 100")
    @Max(value = 100, message = "Auto-reject threshold must be between 0 and 100")
    private Integer autoRejectThreshold;

    @NotNull(message = "Auto-pass threshold is required")
    @Min(value = 0, message = "Auto-pass threshold must be between 0 and 100")
    @Max(value = 100, message = "Auto-pass threshold must be between 0 and 100")
    private Integer autoPassThreshold;

    private Set<String> skillIds;

    @Valid
    private List<JobQuestionRequest> questions;

    public JobListingRequest(
            String title,
            JobType type,
            String location,
            String summary,
            String responsibilities,
            String requiredQualifications,
            String preferredQualifications,
            JobStatus status,
            Integer autoRejectThreshold,
            Integer autoPassThreshold,
            Set<String> skillIds
    ) {
        this(
                title,
                type,
                location,
                summary,
                responsibilities,
                requiredQualifications,
                preferredQualifications,
                status,
                autoRejectThreshold,
                autoPassThreshold,
                skillIds,
                null
        );
    }
}
