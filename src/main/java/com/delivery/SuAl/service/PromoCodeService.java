package com.delivery.SuAl.service;

import com.delivery.SuAl.helper.PromoDiscountResult;

import java.math.BigDecimal;

public interface PromoCodeService {
    PromoDiscountResult applyPromoCode(String promoCode, BigDecimal subtotal);
}
