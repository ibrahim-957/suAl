package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Admin;
import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.AdminMapper;
import com.delivery.SuAl.model.request.admin.CreateAdminRequest;
import com.delivery.SuAl.model.request.admin.UpdateAdminRequest;
import com.delivery.SuAl.model.response.admin.AdminResponse;
import com.delivery.SuAl.model.response.customer.CustomerResponse;
import com.delivery.SuAl.model.response.operation.OperatorResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final AdminRepository adminRepository;
    private final AdminMapper adminMapper;

    @Override
    public AdminResponse createAdmin(CreateAdminRequest request) {
        log.info("Creating new admin with email {}", request.getEmail());

        if (adminRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AlreadyExistsException("Email already exists: " + request.getEmail());
        }

        Admin admin = adminMapper.toEntity(request);
        Admin savedAdmin = adminRepository.save(admin);
        return adminMapper.toResponse(savedAdmin);
    }

    @Override
    public AdminResponse updateAdmin(Long id, UpdateAdminRequest request) {
        log.info("Updating admin with id {}", id);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin with id " + id + " not found"));
        if (request.getEmail() != null && !request.getEmail().equals(admin.getUser().getEmail())) {
            adminRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                throw new AlreadyExistsException("Operator already exists with email: " + request.getEmail());
            });
        }

        adminMapper.updateEntityFromRequest(request, admin);
        Admin updatedAdmin = adminRepository.save(admin);
        return adminMapper.toResponse(updatedAdmin);
    }

    @Override
    public AdminResponse getAdminById(Long id) {
        log.info("Getting admin with id {}", id);

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin with id " + id + " not found"));
        return adminMapper.toResponse(admin);
    }

    @Override
    public void deleteAdmin(Long id) {
        adminRepository.deleteById(id);
        log.info("Deleting admin with id {}", id);
    }

    @Override
    public PageResponse<AdminResponse> getAllAdmins(Pageable pageable) {
        log.info("Getting all admins with page: {}", pageable);
        Page<Admin> adminPage = adminRepository.findAll(pageable);

        List<AdminResponse> responses = adminPage.getContent().stream()
                .map(adminMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, adminPage);
    }
}
