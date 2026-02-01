package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.model.request.auth.AuthenticationRequest;
import com.delivery.SuAl.model.request.auth.RefreshTokenRequest;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
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
    public AuthenticationResponse createUser(String identifier, String password, UserRole role, Long targetId) {
        log.info("Creating user for role {} with targetId {}", role, targetId);

        if (role == UserRole.CUSTOMER) {
            if (userRepository.existsByPhoneNumber(identifier)) {
                throw new AlreadyExistsException("Phone number already registered: " + identifier);
            }
        } else {
            if (userRepository.existsByEmail(identifier)) {
                throw new AlreadyExistsException("Email already registered: " + identifier);
            }
        }

        User.UserBuilder userBuilder = User.builder()
                .password(passwordEncoder.encode(password))
                .role(role)
                .targetId(targetId);

        if (role == UserRole.CUSTOMER) {
            userBuilder.phoneNumber(identifier);
        } else {
            userBuilder.email(identifier);
        }

        User user = userBuilder.build();
        User savedUser = userRepository.save(user);

        log.info("User created successfully with ID: {}", savedUser.getId());

        var jwtToken = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(savedUser.getId())
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
}
