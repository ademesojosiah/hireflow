package com.hireflow.hireflow.company.service;

import com.hireflow.hireflow.company.dto.request.CompanyRequest;
import com.hireflow.hireflow.company.dto.response.CompanyResponse;
import com.hireflow.hireflow.company.entity.Company;
import com.hireflow.hireflow.data.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyService {

    CompanyResponse create(CompanyRequest request, User user);

    CompanyResponse update(String id, CompanyRequest request, User user);

    CompanyResponse findById(String id);

    Page<CompanyResponse> findAll(Pageable pageable);

    Company findCompanyById(String id);

    void delete(String id, User user);

    CompanyResponse getMyCompany(User user);
}
