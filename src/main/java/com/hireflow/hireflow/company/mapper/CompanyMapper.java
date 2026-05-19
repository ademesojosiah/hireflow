package com.hireflow.hireflow.company.mapper;

import com.hireflow.hireflow.company.dto.request.CompanyRequest;
import com.hireflow.hireflow.company.dto.response.CompanyResponse;
import com.hireflow.hireflow.company.entity.Company;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyMapper {

    private final ModelMapper modelMapper;

    public Company toEntity(CompanyRequest request) {
        return modelMapper.map(request, Company.class);
    }

    public CompanyResponse toResponse(Company company) {
        return modelMapper.map(company, CompanyResponse.class);
    }

    public void applyUpdate(Company company, CompanyRequest request) {
        if (request.getName() != null) company.setName(request.getName());
        if (request.getIndustry() != null) company.setIndustry(request.getIndustry());
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite());
        if (request.getLogoUrl() != null) company.setLogoUrl(request.getLogoUrl());
        if (request.getCompanySize() != null) company.setCompanySize(request.getCompanySize());
    }
}
