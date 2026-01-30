package com.delivery.SuAl.service;

import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.model.request.auth.AuthenticationRequest;
import com.delivery.SuAl.model.request.auth.RefreshTokenRequest;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;

public interface AuthenticationService {
    AuthenticationResponse createUser(String identifier, String password, UserRole role, Long targetId);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    void deleteUser(Long targetId, UserRole role);
}
