package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.AffordablePackage;
import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.CustomerContainer;
import com.delivery.SuAl.entity.CustomerPackageOrder;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.exception.InsufficientContainerException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.helper.ProductDepositInfo;
import com.delivery.SuAl.model.request.order.BottleCollectionItem;
import com.delivery.SuAl.repository.AffordablePackageRepository;
import com.delivery.SuAl.repository.CustomerContainerRepository;
import com.delivery.SuAl.repository.CustomerRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContainerManagementServiceImpl implements ContainerManagementService {
    private final CustomerContainerRepository customerContainerRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final AffordablePackageRepository affordablePackageRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public ContainerDepositSummary calculateAvailableContainerRefunds(
            Long customerId,
            Map<Long, Integer> productQuantities) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new IllegalArgumentException("Product quantities cannot be empty");
        }

        log.info("Calculating container refunds for customer {}", customerId);

        List<ProductDepositInfo> productDepositInfos = new ArrayList<>();
        int totalContainersUsed = 0;
        BigDecimal totalDepositRefunded = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer orderQuantity = entry.getValue();

            CustomerContainer customerContainer = customerContainerRepository
                    .findByCustomerIdAndProductId(customerId, productId)
                    .orElse(null);

            int availableContainers = (customerContainer != null) ? customerContainer.getQuantity() : 0;

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product not found with id " + productId));

            BigDecimal depositPerUnit = product.getDepositAmount() != null
                    ? product.getDepositAmount()
                    : BigDecimal.ZERO;

            int containersToUse = Math.min(availableContainers, orderQuantity);

            BigDecimal refundForProduct = depositPerUnit
                    .multiply(BigDecimal.valueOf(availableContainers))
                    .setScale(2, RoundingMode.HALF_UP);

            productDepositInfos.add(new ProductDepositInfo(
                    productId,
                    orderQuantity,
                    availableContainers,
                    containersToUse,
                    depositPerUnit,
                    refundForProduct
            ));

            totalContainersUsed += availableContainers;
            totalDepositRefunded = totalDepositRefunded.add(refundForProduct);

            log.debug("Product {}: ordered={}, available={}, using={}, refund={}",
                    productId, orderQuantity, availableContainers, availableContainers, refundForProduct);
        }

        log.info("Total containers to use: {}, Total refund: {}",
                totalContainersUsed, totalDepositRefunded);

        return new ContainerDepositSummary(
                productDepositInfos,
                totalContainersUsed,
                totalDepositRefunded
        );
    }

    @Override
    @Transactional
    public void reserveContainers(Long customerId, ContainerDepositSummary depositSummary) {
        log.info("Reserving containers for customer {}", customerId);

        if (depositSummary == null || depositSummary.getProductDepositInfos().isEmpty()) {
            log.debug("No containers to reserve");
            return;
        }

        for (ProductDepositInfo info : depositSummary.getProductDepositInfos()) {
            if (info.getContainersUsed() > 0) {
                CustomerContainer container = customerContainerRepository
                        .findByCustomerIdAndProductIdWithLock(customerId, info.getProductId())
                        .orElseThrow(() -> new InsufficientContainerException(
                                String.format("Customer %d does not have containers for product %d",
                                        customerId, info.getProductId())));

                int newQuantity = container.getQuantity() - info.getContainersUsed();

                if (newQuantity < 0) {
                    throw new InsufficientContainerException(
                            String.format("Customer %d does not have enough containers for product %d. " +
                                            "Available: %d, Required: %d",
                                    customerId, info.getProductId(), container.getQuantity(), info.getContainersUsed())
                    );
                }

                container.setQuantity(newQuantity);
                container.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                customerContainerRepository.save(container);

                log.info("Reserved {} containers for product {}. Remaining: {}",
                        info.getContainersUsed(), info.getProductId(), newQuantity);
            }
        }
        log.info("Successfully reserved {} total containers for customer {}",
                depositSummary.getTotalContainersUsed(), customerId);
    }

    @Override
    @Transactional
    public void releaseReservedContainers(Order order) {
        log.info("Releasing reserved containers for order {}", order.getOrderNumber());

        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            log.debug("No order details to release containers for");
            return;
        }

        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.getContainersReturned() > 0) {
                CustomerContainer container = getOrCreateContainer(
                        order.getCustomer().getId(),
                        detail.getProduct().getId()
                );

                container.setQuantity(container.getQuantity() + detail.getContainersReturned());
                container.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                customerContainerRepository.save(container);

                log.info("Released {} containers for product {} back to customer {}",
                        detail.getContainersReturned(), detail.getProduct().getId(),
                        order.getCustomer().getId());
            }
        }
    }

    @Override
    @Transactional
    public void processOrderCompletion(
            Long customerId,
            List<OrderDetail> orderDetails,
            List<BottleCollectionItem> bottlesCollected
    ) {
        log.info("Processing order completion for customer {}", customerId);

        processDeliveredProducts(customerId, orderDetails);

        processCollectedBottles(customerId, orderDetails, bottlesCollected);

        log.info("Order completion processed successfully for customer {}", customerId);
    }

    @Override
    @Transactional
    public void processCollectedBottles(
            Long customerId,
            List<OrderDetail> orderDetails,
            List<BottleCollectionItem> bottlesCollected) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));

        if (bottlesCollected == null || bottlesCollected.isEmpty()) {
            log.info("No bottles collected for customer {}", customerId);
            for (OrderDetail detail : orderDetails) {
                detail.setContainersReturned(0);
            }
            return;
        }

        log.info("Processing {} collected bottles for customer {}",
                bottlesCollected.size(), customerId);

        Map<Long, Integer> collectedByProduct = new HashMap<>();
        for (BottleCollectionItem item : bottlesCollected) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();
            collectedByProduct.put(productId, collectedByProduct.getOrDefault(productId, 0) + quantity);
        }

        for (OrderDetail detail : orderDetails) {
            Long productId = detail.getProduct().getId();
            Integer actualCollected = collectedByProduct.getOrDefault(productId, 0);

            detail.setContainersReturned(actualCollected);

            if (actualCollected > 0) {
                removeFromCustomerContainer(customer, detail.getProduct(), actualCollected);
                log.info("Customer {}: Removed {} containers of product {} from balance",
                        customerId, actualCollected, productId);
            }
        }

        customerRepository.save(customer);

        Map<Long, Integer> bottlesToAddToWarehouse = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : collectedByProduct.entrySet()) {
            if (entry.getValue() > 0) {
                bottlesToAddToWarehouse.put(entry.getKey(), entry.getValue());
            }
        }

        if (!bottlesToAddToWarehouse.isEmpty()) {
            inventoryService.addEmptyBottlesBatch(bottlesToAddToWarehouse);
        }

        int totalCollected = bottlesCollected.stream()
                .mapToInt(BottleCollectionItem::getQuantity)
                .sum();

        log.info("Completed bottle collection for customer {}: {} bottles collected across {} products",
                customerId, totalCollected, collectedByProduct.size());
    }

    private void removeFromCustomerContainer(Customer customer, Product product, Integer quantity) {

        CustomerContainer existing = customerContainerRepository
                .findByCustomerIdAndProductIdWithLock(customer.getId(), product.getId())
                .orElse(null);

        if (existing != null) {
            int previousQuantity = existing.getQuantity();
            int newQuantity = previousQuantity - quantity;

            if (newQuantity < 0) {
                throw new InsufficientContainerException(
                        String.format("Cannot remove %d containers for product %d. Customer only has %d",
                                quantity, product.getId(), previousQuantity)
                );
            }

            existing.setQuantity(newQuantity);
            existing.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            log.debug("Customer {}: Removed {} containers for product {}. Previous: {}, New: {}",
                    customer.getId(), quantity, product.getId(), previousQuantity, newQuantity);

            if (newQuantity == 0) {
                customer.getCustomerContainers().remove(existing);
                customerContainerRepository.delete(existing);
                log.debug("Customer {}: Removed empty container record for product {}",
                        customer.getId(), product.getId());
            } else {
                customerContainerRepository.save(existing);
            }
        } else {
            throw new InsufficientContainerException(
                    String.format("Customer %d has no container record for product %d",
                            customer.getId(), product.getId())
            );
        }
    }

    @Override
    @Transactional
    public void processDeliveredProducts(Long customerId, List<OrderDetail> orderDetails) {
        log.info("Processing delivered products for customer {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));

        int totalDelivered = 0;

        for (OrderDetail detail : orderDetails) {
            int deliveredQuantity = detail.getCount();

            if (deliveredQuantity > 0) {
                addOrUpdateCustomerContainer(customer, detail.getProduct(), deliveredQuantity);
                totalDelivered += deliveredQuantity;

                log.debug("Customer {}: Added {} containers of product {} ({})",
                        customerId, deliveredQuantity,
                        detail.getProduct().getId(),
                        detail.getProduct().getName());
            }
        }

        customerRepository.save(customer);

        log.info("Completed delivery processing for customer {}: {} bottles delivered across {} products",
                customerId, totalDelivered, orderDetails.size());
    }

    private void addOrUpdateCustomerContainer(Customer customer, Product product, Integer quantity) {

        Optional<CustomerContainer> existing = customerContainerRepository
                .findByCustomerIdAndProductIdWithLock(customer.getId(), product.getId());

        if (existing.isPresent()) {
            CustomerContainer container = existing.get();
            int previousQuantity = container.getQuantity();

            long newCount = (long) previousQuantity + quantity;
            if (newCount > Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        String.format("Container count overflow for customer %d, product %d",
                                customer.getId(), product.getId())
                );
            }

            container.setQuantity(previousQuantity + quantity);
            container.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            customerContainerRepository.save(container);

            log.debug("Customer {}: Updated container for product {}. Previous: {}, New: {}",
                    customer.getId(), product.getId(), previousQuantity, container.getQuantity());
        } else {
            CustomerContainer newContainer = new CustomerContainer();
            newContainer.setCustomer(customer);
            newContainer.setProduct(product);
            newContainer.setQuantity(quantity);
            newContainer.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            newContainer.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            customerContainerRepository.save(newContainer);

            log.debug("Customer {}: Created new container for product {} with quantity {}",
                    customer.getId(), product.getId(), quantity);
        }
    }

    @Override
    @Transactional
    public CustomerContainer getOrCreateContainer(Long customerId, Long productId) {
        return customerContainerRepository
                .findByCustomerIdAndProductId(customerId, productId)
                .orElseGet(() -> {
                    log.debug("Creating new container record for customer {} and product {}",
                            customerId, productId);

                    Customer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));

                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

                    CustomerContainer newContainer = new CustomerContainer();
                    newContainer.setCustomer(customer);
                    newContainer.setProduct(product);
                    newContainer.setQuantity(0);
                    newContainer.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    newContainer.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

                    return customerContainerRepository.save(newContainer);
                });
    }

    @Override
    @Transactional
    public void processPackageOrderCompletion(CustomerPackageOrder packageOrder) {
        AffordablePackage pkg = packageOrder.getAffordablePackage();
        Customer customer = packageOrder.getCustomer();
        int totalBottles = pkg.getTotalContainers() * packageOrder.getFrequency();

        log.info("Processing package order completion. Customer: {}, Package: {}, Bottles needed: {}",
                customer.getId(), pkg.getId(), totalBottles);

        int totalAvailable = customer.getCustomerContainers().stream()
                .mapToInt(CustomerContainer::getQuantity)
                .sum();

        if (totalAvailable < totalBottles) {
            throw new InsufficientContainerException(
                    String.format("Customer does not have enough containers. Required: %d, Available: %d",
                            totalBottles, totalAvailable)
            );
        }

        int remaining = totalBottles;
        List<CustomerContainer> containers = new ArrayList<>(customer.getCustomerContainers());

        for (CustomerContainer container : containers) {
            if (remaining <= 0) break;

            int toDeduct = Math.min(container.getQuantity(), remaining);
            container.setQuantity(container.getQuantity() - toDeduct);
            container.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

            if (container.getQuantity() == 0) {
                customer.getCustomerContainers().remove(container);
                customerContainerRepository.delete(container);
                log.debug("Removed empty container record for product {}", container.getProduct().getId());
            } else {
                customerContainerRepository.save(container);
            }

            remaining -= toDeduct;
        }
        log.info("Package order containers processed. Customer: {}, Total deducted: {}",
                customer.getId(), totalBottles);
    }

    @Override
    @Transactional
    public boolean validatePackageContainerAvailability(
            Long customerId, Long packageId, int quantity) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));

        AffordablePackage pkg = affordablePackageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("Package not found with id: " + packageId));

        int requiredContainers = pkg.getTotalContainers() * quantity;
        int availableContainers = customer.getCustomerContainers().stream()
                .mapToInt(CustomerContainer::getQuantity)
                .sum();

        return availableContainers >= requiredContainers;
    }

    @Override
    @Transactional
    public int calculatePackageContainerRequirement(Long packageId, int quantity) {
        AffordablePackage pkg = affordablePackageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("Package not found with id: " + packageId));
        return pkg.getTotalContainers() * quantity;
    }
}