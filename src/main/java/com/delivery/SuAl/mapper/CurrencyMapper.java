package com.delivery.SuAl.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CurrencyMapper {
    private static final Map<String, String> CURRENCY_TO_NUMERIC = Map.of(
            "AZN", "944"
    );

    private static final Map<String, String> NUMERIC_TO_CURRENCY = Map.of(
            "944", "AZN"
    );

    @Named("toNumeric")
    public static String toNumericCode(String currencyCode) {
        if (currencyCode == null) return null;
        return CURRENCY_TO_NUMERIC.getOrDefault(currencyCode.toUpperCase(), currencyCode);
    }

    public static String toAlphaCode(String numericCode) {
        if (numericCode == null) return null;
        return NUMERIC_TO_CURRENCY.getOrDefault(numericCode, numericCode);
    }
}
