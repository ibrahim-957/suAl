package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.model.request.companyAndcategory.CreateCompanyRequest;
import com.delivery.SuAl.model.request.companyAndcategory.UpdateCompanyRequest;
import com.delivery.SuAl.model.response.companyAndcategory.CompanyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    Company toEntity(CreateCompanyRequest createCompanyRequest);

    void updateEntityFromRequest(UpdateCompanyRequest updateCompanyRequest,
                                 @MappingTarget Company company);

//    @Mapping(target = "productCount", ignore = true)
    CompanyResponse toResponse(Company company);

    List<CompanyResponse> toResponseList(List<Company> companyList);
}
