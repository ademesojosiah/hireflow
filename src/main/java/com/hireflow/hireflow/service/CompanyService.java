package com.hireflow.hireflow.service;

import com.hireflow.hireflow.data.model.Company;
import com.hireflow.hireflow.data.model.User;
import com.hireflow.hireflow.dto.request.CompanyRequest;
import com.hireflow.hireflow.dto.response.CompanyResponse;
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
