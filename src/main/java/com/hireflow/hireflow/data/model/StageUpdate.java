package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hireflow.hireflow.enums.ApplicationStage;
import com.hireflow.hireflow.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "stage_updates",
        indexes = @Index(name = "idx_stage_update_application", columnList = "application_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StageUpdate extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column
    private ApplicationStage previousStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStage currentStage;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String actor;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "actor_email")
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role")
    private Role actorRole;
}
