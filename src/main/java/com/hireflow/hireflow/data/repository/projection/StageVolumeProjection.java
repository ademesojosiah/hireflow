package com.hireflow.hireflow.data.repository.projection;

import com.hireflow.hireflow.enums.ApplicationStage;

public interface StageVolumeProjection {
    ApplicationStage getStage();
    Long getCount();
}
