package com.delivery.SuAl.helper;

import com.delivery.SuAl.entity.Promo;
import lombok.Value;

import java.math.BigDecimal;

@Value
public class PromoDiscountResult{
    Promo promo;
    BigDecimal discount;

    public static PromoDiscountResult noDiscount(){
        return new PromoDiscountResult(null,BigDecimal.ZERO);
    }

    public boolean hasPromo(){
        return promo != null;
    }
}
