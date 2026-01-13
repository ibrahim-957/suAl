package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.CompanyMapper;
import com.delivery.SuAl.model.enums.CompanyStatus;
import com.delivery.SuAl.model.request.companyAndcategory.CreateCompanyRequest;
import com.delivery.SuAl.model.request.companyAndcategory.UpdateCompanyRequest;
import com.delivery.SuAl.model.response.companyAndcategory.CompanyResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CompanyRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final CompanyMapper companyMapper;

    @Override
    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest createCompanyRequest) {
        log.info("Creating new company with name: {}", createCompanyRequest.getName());

        if (companyRepository.findByName(createCompanyRequest.getName()).isPresent())
            throw new AlreadyExistsException("Company with name " + createCompanyRequest.getName() + " already exists");

        Company company = companyMapper.toEntity(createCompanyRequest);
        Company savedCompany = companyRepository.save(company);

        CompanyResponse companyResponse = companyMapper.toResponse(savedCompany);
        companyResponse.setProductCount(0L);

        log.info("Company created successfully with ID: {}", savedCompany.getId());
        return companyResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        log.info("Getting company with ID: {}", id);

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + id));

        CompanyResponse companyResponse = companyMapper.toResponse(company);
        companyResponse.setProductCount(productRepository.countByCompanyId(id));
        return companyResponse;
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(Long id, UpdateCompanyRequest updateCompanyRequest) {
        log.info("Updating company with ID: {}", id);

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + id));

        if (updateCompanyRequest.getName() != null && !updateCompanyRequest.getName().equals(company.getName())) {
            companyRepository.findByName(updateCompanyRequest.getName()).ifPresent(existing -> {
                throw new AlreadyExistsException("Company already exists with name: " + updateCompanyRequest.getName());
            });
        }

        companyMapper.updateEntityFromRequest(updateCompanyRequest, company);
        Company updatedCompany = companyRepository.save(company);

        CompanyResponse companyResponse = companyMapper.toResponse(updatedCompany);
        companyResponse.setProductCount(productRepository.countByCompanyId(id));

        log.info("Company updated successfully with ID: {}", updatedCompany.getId());
        return companyResponse;
    }

    @Override
    public void deleteCompany(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + id));

        company.setCompanyStatus(CompanyStatus.DEACTIVATED);
        companyRepository.save(company);
        log.info("Company deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> getAllCompanies(Pageable pageable) {
        log.info("Getting all companies");

        Page<Company> companyPage = companyRepository.findAll(pageable);

        List<CompanyResponse> responses = companyPage.getContent().stream()
                .map(company -> {
                    CompanyResponse companyResponse = companyMapper.toResponse(company);
                    companyResponse.setProductCount(productRepository.countByCompanyId(company.getId()));
                    return companyResponse;
                })
                .collect(Collectors.toList());
        return PageResponse.of(responses, companyPage);
    }
}