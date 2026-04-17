package com.delivery.SuAl.security;

import com.delivery.SuAl.model.enums.OperatorType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OperatorContext {
    private static final ThreadLocal<OperatorInfo> currentOperator = new ThreadLocal<>();

    public static void setCurrentOperator(OperatorInfo operatorInfo) {
        currentOperator.set(operatorInfo);
        if (operatorInfo != null) {
            log.debug("Operator context set: {} ({}), Company: {}",
                    operatorInfo.getEmail(),
                    operatorInfo.getOperatorType(),
                    operatorInfo.getCompanyId());
        }
    }

    public static OperatorInfo getCurrentOperator() {
        return currentOperator.get();
    }

    public static void clear(){
        currentOperator.remove();
    }

    public static boolean isSystemOperator() {
        OperatorInfo operatorInfo = getCurrentOperator();
        return operatorInfo != null && operatorInfo.getOperatorType() == OperatorType.SYSTEM;
    }

    public static boolean isSupplierOperator() {
        OperatorInfo operatorInfo = getCurrentOperator();
        return operatorInfo != null && operatorInfo.getOperatorType() == OperatorType.SUPPLIER;
    }

    public static Long getCurrentCompanyId() {
        OperatorInfo operatorInfo = getCurrentOperator();
        return operatorInfo != null ? operatorInfo.getCompanyId() : null;
    }

    public static boolean hasAccessToCompany(Long companyId) {
        if (isSystemOperator()){
            return true;
        }

        Long currentCompanyId = getCurrentCompanyId();
        return currentCompanyId != null && companyId.equals(currentCompanyId);
    }
}