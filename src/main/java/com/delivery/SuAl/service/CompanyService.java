package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.companyAndcategory.CreateCompanyRequest;
import com.delivery.SuAl.model.request.companyAndcategory.UpdateCompanyRequest;
import com.delivery.SuAl.model.response.companyAndcategory.CompanyResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public interface CompanyService {
    CompanyResponse createCompany(CreateCompanyRequest createCompanyRequest);

    CompanyResponse getCompanyById(Long id);

    CompanyResponse updateCompany(Long id, UpdateCompanyRequest updateCompanyRequest);

    void deleteCompany(Long id);

    PageResponse<CompanyResponse> getAllCompanies(Pageable pageable);
}
