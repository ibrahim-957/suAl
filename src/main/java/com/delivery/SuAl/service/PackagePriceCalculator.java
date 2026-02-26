package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.AffordablePackage;
import com.delivery.SuAl.entity.AffordablePackageProduct;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.ProductPrice;
import com.delivery.SuAl.repository.ProductPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PackagePriceCalculator {
    private final ProductPriceRepository productPriceRepository;

    public BigDecimal getActiveSellPrice(Product  product) {
        if (product == null) return BigDecimal.ZERO;
        return productPriceRepository.findActiveByProductId(product.getId())
                .map(ProductPrice::getEffectiveSellPrice)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal calculateLineValue(AffordablePackageProduct packageProduct) {
        BigDecimal price = getActiveSellPrice(packageProduct.getProduct());
        return price
                .multiply(BigDecimal.valueOf(packageProduct.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateOriginalValue(List<AffordablePackageProduct> packageProducts) {
        if (packageProducts == null || packageProducts.isEmpty()) return BigDecimal.ZERO;
        return packageProducts.stream()
                .map(this::calculateLineValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateProfit(AffordablePackage affordablePackage) {
        BigDecimal originalValue = calculateOriginalValue(affordablePackage.getPackageProducts());
        if (originalValue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return originalValue
                .subtract(affordablePackage.getTotalPrice())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
