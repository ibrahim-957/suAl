package com.delivery.SuAl.util;

import com.delivery.SuAl.model.enums.ReceiverType;
import com.delivery.SuAl.model.enums.UserRole;

public class ReceiverTypeResolver {
    private ReceiverTypeResolver() {
    }

    public static ReceiverType resolve(UserRole role) {
        return switch (role) {
            case CUSTOMER -> ReceiverType.CUSTOMER;
            case OPERATOR -> ReceiverType.OPERATOR;
            case ADMIN -> ReceiverType.ADMIN;
            default -> throw new IllegalArgumentException("Invalid role");
        };
    }
}
