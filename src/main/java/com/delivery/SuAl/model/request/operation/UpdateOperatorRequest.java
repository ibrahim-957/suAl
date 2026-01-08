package com.delivery.SuAl.model.request.operation;

import com.delivery.SuAl.model.enums.OperatorStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOperatorRequest {
    private String firstName;

    private String lastName;

    @Email
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotNull
    private OperatorStatus operatorStatus;
}
