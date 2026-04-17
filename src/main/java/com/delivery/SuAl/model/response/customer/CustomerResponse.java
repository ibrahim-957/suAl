package com.delivery.SuAl.model.response.customer;

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
public class CustomerResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Boolean isActive;
    private List<AddressResponse> addresses;
    private List<CustomerContainerResponse> customerContainers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
