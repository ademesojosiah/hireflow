package com.hireflow.hireflow.company.entity;

import com.hireflow.hireflow.data.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "companies", indexes = @Index(name = "idx_company_name", columnList = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Company extends BaseEntity {

   @Column(nullable = false, unique = true)
   private String name;

   @Column(length = 1000)
   private String description;

   @Column
   private String industry;

   @Column
   private String website;

   @Column
   private String logoUrl;

   @Column
   private String companySize;
}
