package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.model.request.operation.CreateOperatorRequest;
import com.delivery.SuAl.model.request.operation.UpdateOperatorRequest;
import com.delivery.SuAl.model.response.operation.OperatorResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OperatorMapper {
    Operator toEntity(CreateOperatorRequest createOperatorRequest);

    void updateEntityFromRequest(UpdateOperatorRequest updateOperatorRequest,
                                 @MappingTarget Operator operator);

    OperatorResponse toResponse(Operator operator);

    List<OperatorResponse> toResponseList(List<Operator> operators);
}
