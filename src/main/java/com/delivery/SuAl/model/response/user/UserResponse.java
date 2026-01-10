package com.delivery.SuAl.model.response.user;

import com.delivery.SuAl.model.response.address.AddressResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Boolean isActive;
    private List<AddressResponse> addresses;
    private List<UserContainerResponse> userContainers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
