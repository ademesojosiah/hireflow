package com.hireflow.hireflow.data.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "skills",
        indexes = @Index(name = "idx_skill_name", columnList = "name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Skill extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;
}
