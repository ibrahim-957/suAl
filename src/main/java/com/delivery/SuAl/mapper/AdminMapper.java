package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Admin;
import com.delivery.SuAl.model.request.admin.CreateAdminRequest;
import com.delivery.SuAl.model.request.admin.UpdateAdminRequest;
import com.delivery.SuAl.model.response.admin.AdminResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AdminMapper {
    Admin toEntity(CreateAdminRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateAdminRequest request, @MappingTarget Admin admin);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.phoneNumber", target = "phoneNumber")
    AdminResponse toResponse(Admin admin);
}
