package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.model.request.auth.AuthenticationRequest;
import com.delivery.SuAl.model.request.auth.ChangePasswordRequest;
import com.delivery.SuAl.model.request.auth.RefreshTokenRequest;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.repository.UserRepository;
import com.delivery.SuAl.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthenticationResponse createUser(String email, String phoneNumber, String password, UserRole role, Long targetId) {
        log.info("Creating user for role {} with targetId {}", role, targetId);

        if (email != null && userRepository.existsByEmail(email)) {
            throw new AlreadyExistsException("Email already registered: " + email);
        }

        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new AlreadyExistsException("Phone number already registered: " + phoneNumber);
        }

        User.UserBuilder userBuilder = User.builder()
                .password(passwordEncoder.encode(password))
                .role(role)
                .targetId(targetId);

        if (role == UserRole.CUSTOMER) {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number is required for CUSTOMER role");
            }
            userBuilder.phoneNumber(phoneNumber);
        } else {
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required for " + role + " role");
            }
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number is required for " + role + " role");
            }
            userBuilder.email(email).phoneNumber(phoneNumber);
        }

        User user = userBuilder.build();
        User savedUser = userRepository.save(user);

        log.info("User created successfully with ID: {} and role: {}", savedUser.getId(), savedUser.getRole());

        var jwtToken = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(savedUser.getId())
                .targetId(savedUser.getTargetId())
                .role(role)
                .build();
    }

    @Deprecated
    @Transactional
    public AuthenticationResponse createUser(String identifier, String password, UserRole role, Long targetId) {
        log.info("Creating user for role {} with targetId {} (legacy method)", role, targetId);

        if (role == UserRole.CUSTOMER) {
            return createUser(null, identifier, password, role, targetId);
        } else {
            log.warn("Using legacy createUser method for non-CUSTOMER role. Consider using the new method signature.");
            return createUser(identifier, null, password, role, targetId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Authenticating user with identifier: {}", request.getIdentifier());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()
                )
        );

        if (request.getIdentifier() != null) {
            User user = userRepository.findByPhoneNumber(request.getIdentifier())
                    .orElseGet(() -> userRepository.findByEmail(request.getIdentifier())
                            .orElseThrow(() -> new NotFoundException("User not found with identifier: " + request.getIdentifier())));

            log.info("User authenticated successfully with ID: {}", user.getId());

            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);

            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .userId(user.getId())
                    .role(user.getRole())
                    .targetId(user.getTargetId())
                    .build();
        } else {
            throw new NotFoundException("User not found");
        }
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        final String refreshToken = request.getRefreshToken();
        final String identifier = jwtService.extractUsername(refreshToken);

        log.info("Refreshing token for identifier: {}", identifier);

        if (identifier != null) {
            User user = userRepository.findByPhoneNumber(identifier)
                    .orElseGet(() -> userRepository.findByEmail(identifier)
                            .orElseThrow(() -> new NotFoundException("User not found with identifier: " + identifier)));

            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);

                log.info("Token refreshed successfully with ID: {}", accessToken);

                return AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenType("Bearer")
                        .expiresIn(3600L)
                        .userId(user.getId())
                        .role(user.getRole())
                        .targetId(user.getTargetId())
                        .build();
            }
        }
        throw new RuntimeException("Invalid refresh token");
    }

    @Override
    public void deleteUser(Long targetId, UserRole role) {
        log.info("Deleting user for role {} with identifier {}", role, targetId);
        userRepository.deleteByTargetIdAndRole(targetId, role);
    }

    @Override
    @Transactional
    public ApiResponse<String> changePassword(ChangePasswordRequest request, Long userId) {
        log.info("Processing password change request for user ID: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())){
            throw new IllegalArgumentException("Passwords don't match");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Failed password change attempt for user ID: {} - incorrect current password", userId);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully with ID: {}", user.getId());

        return ApiResponse.<String>builder()
                .success(true)
                .message("Password changed successfully")
                .data("Your password has been updated")
                .build();
    }
}