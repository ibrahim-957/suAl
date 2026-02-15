package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.CustomerContainer;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.helper.OrderDepositInfo;
import com.delivery.SuAl.helper.PackageDepositSummary;
import com.delivery.SuAl.model.request.affordablepackage.DeliveryDistributionRequest;
import com.delivery.SuAl.model.request.affordablepackage.DeliveryProductRequest;
import com.delivery.SuAl.repository.CustomerContainerRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PackageDepositCalculationService {
    private final CustomerContainerRepository customerContainerRepository;
    private final ProductRepository productRepository;

    public PackageDepositSummary calculatePackageDeposits(
            Long customerId,
            Map<Long, Integer> packageProducts) {

        if (customerId == null || customerId <= 0){
            throw new IllegalArgumentException("Invalid customer ID");
        }

        if (packageProducts == null || packageProducts.isEmpty()){
            throw new IllegalArgumentException("Package products cannot be empty");
        }

        log.info("Calculating package deposits for customer: {}", customerId);

        List<CustomerContainer> customerContainers =
                customerContainerRepository.findByCustomerId(customerId);

        Map<Long, Integer> availableContainers = new HashMap<>();
        for (CustomerContainer customerContainer : customerContainers) {
            availableContainers.put(
                    customerContainer.getProduct().getId(),
                    customerContainer.getQuantity());
        }

        BigDecimal totalDepositCharged = BigDecimal.ZERO;
        BigDecimal expectedDepositRefund = BigDecimal.ZERO;
        int oldContainersToCollect = 0;
        int totalContainers = 0;

        for (Map.Entry<Long, Integer> entry : packageProducts.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity <= 0){
                log.warn("Skipping product {} with non-positive quantity: {}", productId, quantity);
            }

            totalContainers += quantity;

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

            BigDecimal depositPerUnit = product.getDepositAmount();

            BigDecimal depositForProduct = depositPerUnit
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);

            totalDepositCharged = totalDepositCharged.add(depositForProduct);

            int available = availableContainers.getOrDefault(productId, 0);
            int toRefund = Math.min(available, quantity);

            if (toRefund > 0) {
                BigDecimal refundForProduct = depositPerUnit
                        .multiply(BigDecimal.valueOf(toRefund))
                        .setScale(2, RoundingMode.HALF_UP);
                expectedDepositRefund = expectedDepositRefund.add(refundForProduct);
                oldContainersToCollect += toRefund;
            }
            log.debug("Product {}: quantity={}, depositCharged={}, available={}, toRefund={}, refund={}",
                    productId, quantity, depositForProduct, available, toRefund,
                    toRefund > 0 ? depositPerUnit.multiply(BigDecimal.valueOf(toRefund)) : BigDecimal.ZERO);
        }

        BigDecimal netDeposit = totalDepositCharged
                .subtract(expectedDepositRefund)
                .setScale(2, RoundingMode.HALF_UP);

        PackageDepositSummary summary = PackageDepositSummary.builder()
                .totalDepositCharged(totalDepositCharged)
                .expectedDepositRefund(expectedDepositRefund)
                .netDeposit(netDeposit)
                .oldContainersToCollect(oldContainersToCollect)
                .totalContainersInPackage(totalContainers)
                .build();

        log.info("Package deposits: totalCharged={}, expectedRefund={}, netDeposit={}",
                totalDepositCharged, expectedDepositRefund, netDeposit);

        return summary;
    }

    public List<OrderDepositInfo> distributeDepositsAcrossOrders(
            PackageDepositSummary packageSummary,
            List<DeliveryDistributionRequest> distributions) {
        if (distributions == null || distributions.isEmpty()){
            throw new IllegalArgumentException("Distributions cannot be empty");
        }

        log.info("Distributing deposits across {} deliveries", distributions.size());

        List<OrderDepositInfo> result = new ArrayList<>();

        for (int i = 0; i < distributions.size(); i++) {
            DeliveryDistributionRequest dist = distributions.get(i);

            BigDecimal depositCharged = calculateDepositForProducts(dist.getProducts());

            BigDecimal depositRefund;
            int containersToCollect;

            if (i == 0) {
                depositRefund = packageSummary.getExpectedDepositRefund();
                containersToCollect = packageSummary.getOldContainersToCollect();
            } else {
                DeliveryDistributionRequest previousDist =
                        distributions.get(i - 1);

                containersToCollect = previousDist.getProducts().stream()
                        .mapToInt(DeliveryProductRequest::getQuantity)
                        .sum();

                depositRefund = calculateDepositForProducts(previousDist.getProducts());
            }

            BigDecimal netDeposit = depositCharged
                    .subtract(depositRefund)
                    .setScale(2, RoundingMode.HALF_UP);

            OrderDepositInfo depositInfo = OrderDepositInfo.builder()
                    .deliveryNumber(i + 1)
                    .depositCharged(depositCharged)
                    .expectedDepositRefund(depositRefund)
                    .netDeposit(netDeposit)
                    .containersToCollect(containersToCollect)
                    .build();

            result.add(depositInfo);
        }
        return result;
    }

    private BigDecimal calculateDepositForProducts(List<DeliveryProductRequest> products) {
        if (products == null || products.isEmpty()) return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;

        for (DeliveryProductRequest product : products) {
            Product p = productRepository.findById(product.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + product.getProductId()));

            BigDecimal depositForProduct = p.getDepositAmount()
                    .multiply(BigDecimal.valueOf(product.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            total = total.add(depositForProduct);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateProportionalProductAmount(
            BigDecimal packageTotalPrice,
            List<DeliveryProductRequest> deliveryProducts,
            int totalContainersInPackage) {

        if (packageTotalPrice == null || packageTotalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Package total price must be positive");
        }

        if (totalContainersInPackage <= 0) {
            throw new IllegalArgumentException("Total containers must be positive");
        }

        if (deliveryProducts == null || deliveryProducts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int deliveryQuantity = deliveryProducts.stream()
                .mapToInt(DeliveryProductRequest::getQuantity)
                .sum();

        if (deliveryQuantity <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal ratio = BigDecimal.valueOf(deliveryQuantity)
                .divide(BigDecimal.valueOf(totalContainersInPackage), 4, RoundingMode.HALF_UP);

        BigDecimal proportionalAmount = packageTotalPrice
                .multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Proportional amount: deliveryQty={}, totalQty={}, ratio={}, amount={}",
                deliveryQuantity, totalContainersInPackage, ratio, proportionalAmount);

        return proportionalAmount;
    }
}