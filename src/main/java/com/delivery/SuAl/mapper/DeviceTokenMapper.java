package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.DeviceToken;
import com.delivery.SuAl.model.request.notification.DeviceTokenRequest;
import com.delivery.SuAl.model.response.notification.DeviceTokenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeviceTokenMapper {
    DeviceToken toEntity(DeviceTokenRequest deviceTokenRequest);

    DeviceTokenResponse toResponse(DeviceToken deviceToken);

    void updateEntityFromRequest(DeviceTokenRequest request, @MappingTarget DeviceToken deviceToken);

    List<DeviceTokenResponse> toResponseList(List<DeviceToken> deviceTokens);
}
