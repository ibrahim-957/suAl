package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.model.request.operation.CreateDriverRequest;
import com.delivery.SuAl.model.request.operation.UpdateDriverRequest;
import com.delivery.SuAl.model.response.operation.DriverResponse;
import com.delivery.SuAl.model.response.operation.DriverSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DriverMapper {
    Driver toEntity(CreateDriverRequest createDriverRequest);

    void updateEntityFromRequest(UpdateDriverRequest updateDriverRequest,
                                 @MappingTarget Driver driver);

    //@Mapping(target = "available", ignore = true)
    DriverResponse toResponse(Driver driver);

    //@Mapping(target = "available", ignore = true)
    DriverSummaryResponse toSummaryResponse(Driver driver);

    List<DriverResponse> toResponseList(List<Driver> drivers);

    List<DriverSummaryResponse> toSummaryResponseList(List<Driver> drivers);
}
