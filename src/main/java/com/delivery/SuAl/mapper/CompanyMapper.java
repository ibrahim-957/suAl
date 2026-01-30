package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.model.request.companyAndcategory.CreateCompanyRequest;
import com.delivery.SuAl.model.request.companyAndcategory.UpdateCompanyRequest;
import com.delivery.SuAl.model.response.companyAndcategory.CompanyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CompanyMapper {
    Company toEntity(CreateCompanyRequest createCompanyRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateCompanyRequest updateCompanyRequest,
                                 @MappingTarget Company company);

    CompanyResponse toResponse(Company company);

    List<CompanyResponse> toResponseList(List<Company> companyList);
}
