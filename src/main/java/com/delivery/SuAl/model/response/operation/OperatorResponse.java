package com.delivery.SuAl.model.response.operation;

import com.delivery.SuAl.model.enums.OperatorStatus;
import com.delivery.SuAl.model.enums.OperatorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OperatorResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private OperatorStatus operatorStatus;
    private OperatorType operatorType;
    private Long companyId;
    private String companyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
