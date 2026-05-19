package com.hireflow.hireflow.company.controller;

import com.hireflow.hireflow.company.dto.request.CompanyRequest;
import com.hireflow.hireflow.company.dto.response.CompanyResponse;
import com.hireflow.hireflow.company.service.CompanyService;
import com.hireflow.hireflow.dto.response.ApiResponse;
import com.hireflow.hireflow.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CompanyRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CompanyResponse created = companyService.create(request, userPrincipal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Company created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @Valid @RequestBody CompanyRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CompanyResponse updated = companyService.update(id, request, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Company updated successfully", updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Company retrieved", companyService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<?> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Companies retrieved",
                companyService.findAll(PageRequest.of(page, size))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        companyService.delete(id, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Company deleted successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyCompany(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        CompanyResponse company = companyService.getMyCompany(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Company retrieved successfully", company));
    }
}
