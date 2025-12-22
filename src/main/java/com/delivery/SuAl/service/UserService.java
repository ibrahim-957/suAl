package com.delivery.SuAl.service;


import com.delivery.SuAl.model.request.user.CreateUserRequest;
import com.delivery.SuAl.model.request.user.UpdateUserRequest;
import com.delivery.SuAl.model.response.user.UserResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(CreateUserRequest createUserRequest);

    UserResponse updateUser(Long id, UpdateUserRequest updateUserRequest);

    UserResponse getUserById(Long id);

    void deleteUser(Long id);

    PageResponse<UserResponse> getAllUsers(Pageable pageable);
}
