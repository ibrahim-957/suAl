package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Promo;
import com.delivery.SuAl.helper.PromoDiscountResult;
import com.delivery.SuAl.model.DiscountType;
import com.delivery.SuAl.model.PromoStatus;
import com.delivery.SuAl.repository.PromoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromoCodeServiceImpl implements PromoCodeService {
    private final PromoRepository promoRepository;

    public PromoDiscountResult applyPromoCode(String promoCode, BigDecimal subtotal) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return PromoDiscountResult.noDiscount();
        }
        try {
            Promo promo = promoRepository.findByPromoCode(promoCode.trim())
                    .orElseThrow(() -> new RuntimeException("Promo code not found"));

            if (!isPromoValid(promo)) {
                return PromoDiscountResult.noDiscount();
            }

            BigDecimal discount = calculateDiscount(promo, subtotal);
            return new PromoDiscountResult(promo, discount);
        } catch (Exception e) {
            return PromoDiscountResult.noDiscount();
        }
    }

    private boolean isPromoValid(Promo promo) {
        LocalDate now = LocalDate.now();
        return promo.getPromoStatus() == PromoStatus.ACTIVE
                && !promo.getValidFrom().isAfter(now)
                && !promo.getValidTo().isBefore(now);
    }

    private BigDecimal calculateDiscount(Promo promo, BigDecimal subtotal) {
        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = promo.getDiscountType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(promo.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : promo.getDiscountValue();

        if (promo.getMaxDiscount() != null && discount.compareTo(promo.getMaxDiscount()) > 0) {
            discount = promo.getMaxDiscount();
        }

        return discount;
    }
}