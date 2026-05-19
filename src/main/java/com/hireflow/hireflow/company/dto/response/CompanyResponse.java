package com.hireflow.hireflow.company.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private String id;
    private String name;
    private String industry;
    private String website;
    private String logoUrl;
    private String companySize;
}
