package com.hireflow.hireflow.mapper;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.dto.request.CompanyRequest;
import com.hireflow.hireflow.dto.response.CompanyResponse;
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
        modelMapper.map(request, company);
    }
}
