package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.model.request.user.CreateUserRequest;
import com.delivery.SuAl.model.request.user.UpdateUserRequest;
import com.delivery.SuAl.model.response.user.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {UserContainerMapper.class})
public interface UserMapper {
    User toEntity(CreateUserRequest createUserRequest);

    void updateEntityFromRequest(UpdateUserRequest updateUserRequest, @MappingTarget User user);

    UserResponse toResponse(User user);
}
