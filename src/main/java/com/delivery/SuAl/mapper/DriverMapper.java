package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.model.request.operation.CreateDriverRequest;
import com.delivery.SuAl.model.request.operation.UpdateDriverRequest;
import com.delivery.SuAl.model.response.operation.DriverResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DriverMapper {
    Driver toEntity(CreateDriverRequest createDriverRequest);

    void updateEntityFromRequest(UpdateDriverRequest updateDriverRequest,
                                 @MappingTarget Driver driver);

    DriverResponse toResponse(Driver driver);


    List<DriverResponse> toResponseList(List<Driver> drivers);

}
