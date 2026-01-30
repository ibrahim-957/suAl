package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.admin.CreateAdminRequest;
import com.delivery.SuAl.model.request.admin.UpdateAdminRequest;
import com.delivery.SuAl.model.response.admin.AdminResponse;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    AuthenticationResponse createAdmin(CreateAdminRequest request);

    AdminResponse updateAdmin(Long id, UpdateAdminRequest request);

    AdminResponse getAdminById(Long id);

    void deleteAdmin(Long id);

    PageResponse<AdminResponse> getAllAdmins(Pageable pageable);
}
