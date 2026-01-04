package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.UserMapper;
import com.delivery.SuAl.model.request.user.CreateUserRequest;
import com.delivery.SuAl.model.request.user.UpdateUserRequest;
import com.delivery.SuAl.model.response.user.UserResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse createUser(CreateUserRequest createUserRequest) {
        log.info("Creating new user with phone number {}", createUserRequest.getPhoneNumber());
        Optional<User> existingUser = userRepository.findByPhoneNumber(createUserRequest.getPhoneNumber());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getIsActive())
                throw new AlreadyExistsException("User already exists: " + createUserRequest.getPhoneNumber());
            else {
                log.info("Reactivating user with phone number {}", createUserRequest.getPhoneNumber());
                user.setFirstName(createUserRequest.getFirstName());
                user.setLastName(createUserRequest.getLastName());
                user.setIsActive(true);
                User savedUser = userRepository.save(user);
                log.info("User reactivated successfully with ID: {}", savedUser.getId());
                return userMapper.toResponse(savedUser);
            }
        }

        User user = userMapper.toEntity(createUserRequest);
        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        log.info("User created successfully with ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest updateUserRequest) {
        log.info("Updating user with id {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id " + id));

        if (updateUserRequest.getPhoneNumber() != null &&
                !updateUserRequest.getPhoneNumber().equals(user.getPhoneNumber()) &&
                userRepository.existsByPhoneNumber(updateUserRequest.getPhoneNumber())) {
            throw new AlreadyExistsException("Phone number already exists: " + updateUserRequest.getPhoneNumber());
        }

        userMapper.updateEntityFromRequest(updateUserRequest, user);
        User savedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse getUserById(Long id) {
        log.info("Getting user with id {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user with id {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id " + id));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deleted successfully with id: {}", id);
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Getting all users with page {}", pageable);
        Page<User> users = userRepository.findAll(pageable);

        List<UserResponse> responses = users.getContent().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, users);
    }
}