package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.CustomerContainer;
import com.delivery.SuAl.exception.InsufficientContainerException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.helper.ProductDepositInfo;
import com.delivery.SuAl.model.request.order.BottleCollectionItem;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.CustomerContainerRepository;
import com.delivery.SuAl.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContainerManagementServiceImpl implements ContainerManagementService {
    private final CustomerContainerRepository customerContainerRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public ContainerDepositSummary calculateAvailableContainerRefunds(Long customerId, Map<Long, Integer> productQuantities) {
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

            BigDecimal refundForProduct = depositPerUnit
                    .multiply(BigDecimal.valueOf(availableContainers))
                    .setScale(2, RoundingMode.HALF_UP);

            productDepositInfos.add(new ProductDepositInfo(
                    productId,
                    orderQuantity,
                    availableContainers,
                    availableContainers,
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
        for (ProductDepositInfo info : depositSummary.getProductDepositInfos()) {
            if (info.getContainersUsed() > 0) {
                CustomerContainer container = customerContainerRepository
                        .findByCustomerIdAndProductId(customerId, info.getProductId())
                        .orElseThrow(() -> new NotFoundException("Container not found for customer " + customerId + " and product " + info.getProductId()));

                int newQuantity = container.getQuantity() - info.getContainersUsed();

                if (newQuantity < 0) {
                    throw new InsufficientContainerException("Customer " + customerId + " does not have enough containers for product " + info.getProductId());
                }

                container.setQuantity(newQuantity);
                customerContainerRepository.save(container);

                log.debug("Reserved {} containers for product {}. Remaining: {}",
                        info.getContainersUsed(), info.getProductId(), newQuantity);
            }
        }
    }

    @Override
    @Transactional
    public void releaseReservedContainers(Order order) {
        log.info("Releasing reserved containers for order {}", order.getOrderNumber());

        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.getContainersReturned() > 0) {
                CustomerContainer container = getOrCreateContainer(
                        order.getCustomer().getId(),
                        detail.getProduct().getId()
                );

                container.setQuantity(container.getQuantity() + detail.getContainersReturned());
                customerContainerRepository.save(container);

                log.debug("Released {} containers for product {}",
                        detail.getContainersReturned(), detail.getProduct().getId());
            }
        }
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

        log.info("Processing {} collected bottles for customer {}", bottlesCollected.size(), customerId);

        Map<Long, Integer> collectedByProduct = bottlesCollected.stream()
                .collect(Collectors.toMap(
                        BottleCollectionItem::getProductId,
                        BottleCollectionItem::getQuantity,
                        Integer::sum
                ));

        for (OrderDetail detail : orderDetails) {
            Long productId = detail.getProduct().getId();
            Integer actualCollected = collectedByProduct.getOrDefault(productId, 0);

            detail.setContainersReturned(actualCollected);

            if (actualCollected > 0) {
                removeFromCustomerContainer(customer, detail.getProduct(), actualCollected);
            } else {
                log.debug("Customer {}: No containers collected for product {}", customerId, productId);
            }
        }

        customerRepository.save(customer);

        Map<Long, Integer> bottlesToAddToWarehouse = collectedByProduct.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!bottlesToAddToWarehouse.isEmpty()) {
            inventoryService.addEmptyBottlesBatch(bottlesToAddToWarehouse);
        }

        int totalCollected = bottlesCollected.stream()
                .mapToInt(BottleCollectionItem::getQuantity)
                .sum();

        log.info("Completed bottle collection for customer {}: {} bottles collected across {} products",
                customerId, totalCollected, collectedByProduct.size());
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

    private void removeFromCustomerContainer(Customer customer, Product product, Integer quantity) {
        CustomerContainer existing = customer.getCustomerContainers().stream()
                .filter(cc -> cc.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            int previousQuantity = existing.getQuantity();
            int newQuantity = previousQuantity - quantity;

            if (newQuantity < 0) {
                log.warn("Customer {}: Trying to remove {} containers for product {}, but only {} available. Setting to 0.",
                        customer.getId(), quantity, product.getId(), previousQuantity);
                newQuantity = 0;
            }

            existing.setQuantity(newQuantity);
            log.debug("Customer {}: Removed {} containers for product {}. Previous: {}, New: {}",
                    customer.getId(), quantity, product.getId(), previousQuantity, newQuantity);

            if (newQuantity == 0) {
                customer.getCustomerContainers().remove(existing);
                log.debug("Customer {}: Removed empty container record for product {}",
                        customer.getId(), product.getId());
            }
        } else {
            log.warn("Customer {}: Cannot remove containers for product {} - no container record exists",
                    customer.getId(), product.getId());
        }
    }

    private void addOrUpdateCustomerContainer(Customer customer, Product product, Integer quantity) {
        Optional<CustomerContainer> existing = customerContainerRepository
                .findByCustomerIdAndProductId(customer.getId(), product.getId());

        if (existing.isPresent()) {
            CustomerContainer container = existing.get();
            int previousQuantity = container.getQuantity();
            container.setQuantity(previousQuantity + quantity);
            container.setUpdatedAt(LocalDateTime.now());
            customerContainerRepository.save(container);

            log.debug("Customer {}: Updated container for product {}. Previous: {}, New: {}",
                    customer.getId(), product.getId(), previousQuantity, container.getQuantity());
        } else {
            CustomerContainer newContainer = new CustomerContainer();
            newContainer.setCustomer(customer);
            newContainer.setProduct(product);
            newContainer.setQuantity(quantity);
            newContainer.setCreatedAt(LocalDateTime.now());
            newContainer.setUpdatedAt(LocalDateTime.now());
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

                    return customerContainerRepository.save(newContainer);
                });
    }
}