package com.delivery.SuAl.security;

import com.delivery.SuAl.model.enums.OperatorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OperatorInfo {
    private Long operatorId;
    private Long companyId;
    private OperatorType operatorType;
    private String email;
    private String firstName;
    private String lastName;
}
