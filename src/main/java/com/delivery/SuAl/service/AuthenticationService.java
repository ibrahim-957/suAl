package com.delivery.SuAl.service;

import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.model.request.auth.AuthenticationRequest;
import com.delivery.SuAl.model.request.auth.ChangePasswordRequest;
import com.delivery.SuAl.model.request.auth.RefreshTokenRequest;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;

public interface AuthenticationService {
    AuthenticationResponse createUser(String email, String phoneNumber, String password, UserRole role, Long targetId);

    AuthenticationResponse createUser(String identifier, String password, UserRole role, Long targetId);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    void deleteUser(Long targetId, UserRole role);

    ApiResponse<String> changePassword(ChangePasswordRequest request, Long userId);
}
