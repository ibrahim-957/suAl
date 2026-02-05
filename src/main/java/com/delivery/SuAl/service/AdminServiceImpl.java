package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Admin;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.AdminMapper;
import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.model.request.admin.CreateAdminRequest;
import com.delivery.SuAl.model.request.admin.UpdateAdminRequest;
import com.delivery.SuAl.model.response.admin.AdminResponse;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.AdminRepository;
import com.delivery.SuAl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final AdminRepository adminRepository;
    private final AdminMapper adminMapper;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AuthenticationResponse createAdmin(CreateAdminRequest request) {
        log.info("Creating new admin with email {}", request.getEmail());

        if (adminRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AlreadyExistsException("Email already exists: " + request.getEmail());
        }

        Admin admin = new Admin();
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());

        AuthenticationResponse response = authenticationService.createUser(
                request.getEmail(),
                request.getPhoneNumber(),
                request.getPassword(),
                UserRole.ADMIN,
                null
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found after creation"));

        admin.setUser(user);

        Admin savedAdmin = adminRepository.save(admin);
        log.info("Admin entity created with ID: {}", savedAdmin.getId());

        user.setTargetId(savedAdmin.getId());
        userRepository.save(user);

        response.setTargetId(savedAdmin.getId());


        log.info("Admin created successfully with ID: {} and linked to User", savedAdmin.getId());

        return response;
    }

    @Override
    @Transactional
    public AdminResponse updateAdmin(Long id, UpdateAdminRequest request) {
        log.info("Updating admin with id {}", id);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin with id " + id + " not found"));

        if (request.getEmail() != null &&
                !request.getEmail().equals(admin.getUser().getEmail())) {

            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AlreadyExistsException("Admin already exists with email: " + request.getEmail());
            }

            admin.getUser().setEmail(request.getEmail());
            userRepository.save(admin.getUser());
        }

        adminMapper.updateEntityFromRequest(request, admin);
        Admin updatedAdmin = adminRepository.save(admin);

        log.info("Admin updated successfully with ID: {}", updatedAdmin.getId());
        return adminMapper.toResponse(updatedAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponse getAdminById(Long id) {
        log.info("Getting admin with id {}", id);

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin with id " + id + " not found"));
        return adminMapper.toResponse(admin);
    }

    @Override
    @Transactional
    public void deleteAdmin(Long id) {
        log.info("Deleting admin with id {}", id);

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin with id " + id + " not found"));

        if (admin.getUser() != null) {
            userRepository.delete(admin.getUser());
            log.info("Deleted User associated with Admin {}", id);
        }

        adminRepository.deleteById(id);
        log.info("Admin deleted successfully with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminResponse> getAllAdmins(Pageable pageable) {
        log.info("Getting all admins with page: {}", pageable);
        Page<Admin> adminPage = adminRepository.findAll(pageable);

        List<AdminResponse> responses = adminPage.getContent().stream()
                .map(adminMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, adminPage);
    }
}