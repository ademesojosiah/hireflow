package com.hireflow.hireflow.data.repository.projection;

import java.time.Instant;

public interface TimeToHireProjection {
    Instant getAppliedAt();
    Instant getHiredAt();
}
