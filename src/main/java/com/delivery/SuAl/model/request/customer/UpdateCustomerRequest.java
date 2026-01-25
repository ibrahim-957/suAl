package com.delivery.SuAl.model.request.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCustomerRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
