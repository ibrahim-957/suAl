package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.model.request.operation.CreateOperatorRequest;
import com.delivery.SuAl.model.request.operation.UpdateOperatorRequest;
import com.delivery.SuAl.model.response.operation.OperatorResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OperatorMapper {
    Operator toEntity(CreateOperatorRequest createOperatorRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "operatorType", ignore = true)
    void updateEntityFromRequest(UpdateOperatorRequest updateOperatorRequest,
                                 @MappingTarget Operator operator);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.phoneNumber", target = "phoneNumber")
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.name", target = "companyName")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "updatedAt", qualifiedByName = "utcToBaku")
    OperatorResponse toResponse(Operator operator);

    List<OperatorResponse> toResponseList(List<Operator> operators);

}
