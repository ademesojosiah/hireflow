package com.hireflow.hireflow.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

   @JsonIgnore
   @OneToMany(
           mappedBy = "company",
           cascade = CascadeType.ALL,
           fetch = FetchType.LAZY,
           orphanRemoval = true
   )
   private List<JobListing> jobListings = new ArrayList<>();
}
