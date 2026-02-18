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
    public AuthenticationResponse createUser(String email, String phoneNumber, String password,
                                             UserRole role, Long targetId) {
        log.info("Creating user for role {} with targetId {}", role, targetId);


        if (email != null && userRepository.existsByEmailAndRole(email, role)) {
            throw new AlreadyExistsException("Email already registered for this role: " + email);
        }

        if (phoneNumber != null && userRepository.existsByPhoneNumberAndRole(phoneNumber, role)) {
            throw new AlreadyExistsException("Phone number already registered for this role: " + phoneNumber);
        }

        User.UserBuilder userBuilder = User.builder()
                .role(role)
                .targetId(targetId);

        if (role == UserRole.CUSTOMER) {
            if (phoneNumber == null || phoneNumber.isBlank()) {
                throw new IllegalArgumentException("Phone number is required for CUSTOMER role");
            }
            userBuilder.phoneNumber(phoneNumber);
        } else {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email is required for " + role + " role");
            }
            if (phoneNumber == null || phoneNumber.isBlank()) {
                throw new IllegalArgumentException("Phone number is required for " + role + " role");
            }
            userBuilder
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .password(passwordEncoder.encode(password));
        }

        User savedUser = userRepository.save(userBuilder.build());
        log.info("User created with ID: {} and role: {}", savedUser.getId(), savedUser.getRole());

        String jwtToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

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

        User user = resolveUserByIdentifier(request.getIdentifier());

        log.info("User authenticated with ID: {}", user.getId());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthenticationResponse authenticateCustomerAfterOtp(String phoneNumber) {
        log.info("Issuing JWT for OTP-verified customer with phone ending: {}",
                phoneNumber.substring(Math.max(0, phoneNumber.length() - 4)));

        User user = userRepository.findByPhoneNumberAndRole(phoneNumber, UserRole.CUSTOMER)
                .orElseThrow(() -> new NotFoundException(
                        "Customer not found with phone: " + phoneNumber
                ));

        return buildAuthResponse(user);
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        final String refreshToken = request.getRefreshToken();
        final String identifier = jwtService.extractUsername(refreshToken);

        log.info("Refreshing token for identifier: {}", identifier);

        if (identifier == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        User user = resolveUserByIdentifier(identifier);

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid refresh token");
        }

        log.info("Token refreshed for user ID: {}", user.getId());

        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(user.getId())
                .role(user.getRole())
                .targetId(user.getTargetId())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<String> changePassword(ChangePasswordRequest request, Long userId) {
        log.info("Processing password change for user ID: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("Passwords don't match");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        if (user.getRole() == UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Customers use OTP login and do not have a password");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Failed password change for user ID: {} — incorrect current password", userId);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user ID: {}", userId);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Password changed successfully")
                .data("Your password has been updated")
                .build();
    }

    @Override
    public void deleteUser(Long targetId, UserRole role) {
        log.info("Deleting user for role {} with targetId {}", role, targetId);
        userRepository.deleteByTargetIdAndRole(targetId, role);
    }

    private User resolveUserByIdentifier(String identifier) {
        if (isPhoneNumber(identifier)) {
            return userRepository.findByPhoneNumberAndRole(identifier, UserRole.CUSTOMER)
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + identifier));
        } else {
            return userRepository.findFirstByEmailAndRoleNot(identifier, UserRole.CUSTOMER)
                    .orElseThrow(() -> new NotFoundException("User not found: " + identifier));
        }
    }

    private boolean isPhoneNumber(String identifier) {
        return identifier != null && identifier.replaceAll("^\\+", "").matches("\\d+");
    }

    private AuthenticationResponse buildAuthResponse(User user) {
        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(user.getId())
                .role(user.getRole())
                .targetId(user.getTargetId())
                .build();
    }
}