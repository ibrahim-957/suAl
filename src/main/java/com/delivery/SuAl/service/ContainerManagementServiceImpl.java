package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.AffordablePackage;
import com.delivery.SuAl.entity.ContainerReservation;
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
import com.delivery.SuAl.repository.ContainerReservationRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ContainerManagementServiceImpl implements ContainerManagementService {
    private final CustomerContainerRepository customerContainerRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final AffordablePackageRepository affordablePackageRepository;
    private final ContainerReservationRepository containerReservationRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public ContainerDepositSummary calculateAvailableContainerRefunds(
            Long customerId,
            Map<Long, Integer> productQuantities) {

        validateInputs(customerId, productQuantities);

        log.info("Calculating container refunds for customer {}", customerId);

        List<ProductDepositInfo> productDepositInfos = new ArrayList<>();
        int totalContainersUsed = 0;
        BigDecimal totalDepositRefunded = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer orderQuantity = entry.getValue();

            int totalBalance = getCustomerContainerBalance(customerId, productId);

            Integer alreadyReserved = getReservedContainerCount(customerId, productId);

            int availableContainers = Math.max(0, totalBalance - alreadyReserved);

            Product product = findProductById(productId);
            BigDecimal depositPerUnit = product.getDepositAmount();

            int containersToUse = Math.min(availableContainers, orderQuantity);

            BigDecimal refundForProduct = depositPerUnit
                    .multiply(BigDecimal.valueOf(containersToUse))
                    .setScale(2, RoundingMode.HALF_UP);

            productDepositInfos.add(new ProductDepositInfo(
                    productId,
                    orderQuantity,
                    availableContainers,
                    containersToUse,
                    depositPerUnit,
                    refundForProduct
            ));

            totalContainersUsed += containersToUse;
            totalDepositRefunded = totalDepositRefunded.add(refundForProduct);

            log.debug("Product {}: ordered={}, totalBalance={}, reserved={}, available={}, using={}",
                    productId, orderQuantity, totalBalance, alreadyReserved,
                    availableContainers, containersToUse);
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
    public void processOrderCompletion(
            Long customerId,
            List<OrderDetail> orderDetails,
            List<BottleCollectionItem> bottlesCollected
    ) {
        log.info("Processing order completion for customer {}", customerId);

        addDeliveredContainersToCustomer(customerId, orderDetails);

        removeCollectedContainersFromCustomer(customerId, orderDetails, bottlesCollected);

        addCollectedBottlesToWarehouse(bottlesCollected);

        log.info("Order completion processed successfully for customer {}", customerId);
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
    public CustomerContainer getOrCreateContainer(Long customerId, Long productId) {
        return customerContainerRepository
                .findByCustomerIdAndProductId(customerId, productId)
                .orElseGet(() -> createNewContainer(customerId, productId));
    }

    @Override
    @Transactional
    public void processPackageOrderCompletion(CustomerPackageOrder packageOrder) {
        AffordablePackage pkg = packageOrder.getAffordablePackage();
        Customer customer = packageOrder.getCustomer();
        int totalBottles = pkg.getTotalContainers() * packageOrder.getFrequency();

        log.info("Processing package order completion. Customer: {}, Package: {}, Bottles: {}",
                customer.getId(), pkg.getId(), totalBottles);

        int totalAvailable = getTotalCustomerContainerBalance(customer.getId());

        if (totalAvailable < totalBottles) {
            throw new InsufficientContainerException(
                    String.format("Customer does not have enough containers. Required: %d, Available: %d",
                            totalBottles, totalAvailable)
            );
        }

        deductContainersFromCustomerBalance(customer, totalBottles);
    }

    @Override
    @Transactional
    public boolean validatePackageContainerAvailability(
            Long customerId, Long packageId, int quantity) {
        Customer customer = findCustomerById(customerId);

        AffordablePackage pkg = findPackageById(packageId);

        int requiredContainers = pkg.getTotalContainers() * quantity;
        int availableContainers = getTotalCustomerContainerBalance(customerId);

        boolean hasEnough = availableContainers >= requiredContainers;

        log.info("Package container validation - Customer: {}, Required: {}, Available: {}, Valid: {}",
                customerId, requiredContainers, availableContainers, hasEnough);

        return hasEnough;
    }

    @Override
    @Transactional
    public void reserveContainersForOrder(Order order, ContainerDepositSummary depositSummary) {
        log.info("Reserving containers for order {}", order.getOrderNumber());

        if (depositSummary == null || depositSummary.getProductDepositInfos().isEmpty()) {
            log.debug("No containers to reserve");
            return;
        }

        List<ContainerReservation> reservations = new ArrayList<>();

        for (ProductDepositInfo info : depositSummary.getProductDepositInfos()) {
            Product product = productRepository.findById(info.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product with id " + info.getProductId() + " not found"));
            if (info.getContainersUsed() > 0) {
                ContainerReservation reservation = new ContainerReservation();
                reservation.setCustomer(order.getCustomer());
                reservation.setOrder(order);
                reservation.setProduct(product);
                reservation.setQuantityReserved(info.getContainersUsed());
                reservation.setReservedAt(LocalDateTime.now(ZoneOffset.UTC));
                reservation.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));

                reservations.add(reservation);
            }
        }

        containerReservationRepository.saveAll(reservations);
        log.info("Reserved {} container types for order {}",
                reservations.size(), order.getOrderNumber());
    }

    @Override
    @Transactional
    public void releaseContainerReservations(Long orderId) {
        log.info("Releasing container reservations for order {}", orderId);

        List<ContainerReservation> reservations = containerReservationRepository.findByOrderId(orderId);

        for (ContainerReservation reservation : reservations) {
            reservation.setReleased(true);
            reservation.setReleasedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        containerReservationRepository.saveAll(reservations);
        log.info("Released {} container reservations", reservations.size());
    }


    private void addDeliveredContainersToCustomer(Long customerId, List<OrderDetail> orderDetails) {
        log.debug("Adding delivered containers to customer {}", customerId);

        Customer customer = findCustomerById(customerId);
        int totalDelivered = 0;

        for (OrderDetail detail : orderDetails) {
            int quantity = detail.getCount();

            if (quantity > 0) {
                addContainersToCustomer(customer, detail.getProduct(), quantity);
                totalDelivered += quantity;

                log.debug("Customer {}: Added {} containers of product {} ({})",
                        customerId, quantity, detail.getProduct().getId(),
                        detail.getProduct().getName());
            }
        }

        customerRepository.save(customer);
        log.info("Added {} total containers to customer {}", totalDelivered, customerId);
    }

    private void removeCollectedContainersFromCustomer(
            Long customerId,
            List<OrderDetail> orderDetails,
            List<BottleCollectionItem> bottlesCollected) {

        if (bottlesCollected == null || bottlesCollected.isEmpty()) {
            log.info("No bottles collected from customer {}", customerId);

            for (OrderDetail detail : orderDetails) {
                detail.setContainersReturned(0);
            }
            return;
        }

        log.debug("Removing {} collected container types from customer {}",
                bottlesCollected.size(), customerId);

        Customer customer = findCustomerById(customerId);

        Map<Long, Integer> collectedByProduct = new HashMap<>();
        for (BottleCollectionItem item : bottlesCollected) {
            collectedByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        for (OrderDetail detail : orderDetails) {
            Long productId = detail.getProduct().getId();
            Integer actualCollected = collectedByProduct.getOrDefault(productId, 0);

            detail.setContainersReturned(actualCollected);

            if (actualCollected > 0) {
                removeContainersFromCustomer(customer, detail.getProduct(), actualCollected);

                log.debug("Customer {}: Removed {} containers of product {}",
                        customerId, actualCollected, productId);
            }
        }

        customerRepository.save(customer);

        int totalCollected = bottlesCollected.stream()
                .mapToInt(BottleCollectionItem::getQuantity)
                .sum();

        log.info("Removed {} total containers from customer {}", totalCollected, customerId);
    }

    private void addCollectedBottlesToWarehouse(List<BottleCollectionItem> bottlesCollected) {
        if (bottlesCollected == null || bottlesCollected.isEmpty()) {
            return;
        }

        Map<Long, Integer> bottlesToAdd = new HashMap<>();
        for (BottleCollectionItem item : bottlesCollected) {
            if (item.getQuantity() > 0) {
                bottlesToAdd.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }

        if (!bottlesToAdd.isEmpty()) {
            inventoryService.addEmptyBottlesBatch(bottlesToAdd);
            log.info("Added {} product types to warehouse as empty bottles", bottlesToAdd.size());
        }
    }

    private void addContainersToCustomer(Customer customer, Product product, int quantity) {
        if (quantity <= 0) {
            return;
        }

        CustomerContainer container = customerContainerRepository
                .findByCustomerIdAndProductIdWithLock(customer.getId(), product.getId())
                .orElse(null);

        if (container != null) {
            int newQuantity = container.getQuantity() + quantity;
            container.setQuantity(newQuantity);
            container.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            customerContainerRepository.save(container);

            log.trace("Updated container: product={}, previous={}, added={}, new={}",
                    product.getId(), container.getQuantity() - quantity, quantity, newQuantity);
        } else {
            CustomerContainer newContainer = new CustomerContainer();
            newContainer.setCustomer(customer);
            newContainer.setProduct(product);
            newContainer.setQuantity(quantity);
            newContainer.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            newContainer.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            customerContainerRepository.save(newContainer);

            log.trace("Created new container: product={}, quantity={}",
                    product.getId(), quantity);
        }
    }

    private void removeContainersFromCustomer(Customer customer, Product product, int quantity) {
        if (quantity <= 0) {
            return;
        }

        CustomerContainer container = customerContainerRepository
                .findByCustomerIdAndProductIdWithLock(customer.getId(), product.getId())
                .orElseThrow(() -> new InsufficientContainerException(
                        String.format("Customer %d has no containers for product %d",
                                customer.getId(), product.getId())
                ));

        int previousQuantity = container.getQuantity();
        int newQuantity = previousQuantity - quantity;

        if (newQuantity < 0) {
            throw new InsufficientContainerException(
                    String.format("Cannot remove %d containers for product %d. Customer only has %d",
                            quantity, product.getId(), previousQuantity)
            );
        }

        if (newQuantity == 0) {
            customer.getCustomerContainers().remove(container);
            customerContainerRepository.delete(container);
            log.trace("Deleted empty container record: customerId={}, productId={}",
                    customer.getId(), product.getId());
        } else {
            container.setQuantity(newQuantity);
            container.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            customerContainerRepository.save(container);
            log.trace("Updated container: product={}, previous={}, removed={}, new={}",
                    product.getId(), previousQuantity, quantity, newQuantity);
        }
    }

    private void deductContainersFromCustomerBalance(Customer customer, int totalToDeduct) {
        int remaining = totalToDeduct;
        List<CustomerContainer> containers = new ArrayList<>(customer.getCustomerContainers());

        for (CustomerContainer container : containers) {
            if (remaining <= 0) {
                break;
            }

            int toDeduct = Math.min(container.getQuantity(), remaining);
            int newQuantity = container.getQuantity() - toDeduct;

            container.setQuantity(newQuantity);
            container.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

            if (newQuantity == 0) {
                customer.getCustomerContainers().remove(container);
                customerContainerRepository.delete(container);
                log.debug("Removed empty container: productId={}", container.getProduct().getId());
            } else {
                customerContainerRepository.save(container);
            }

            remaining -= toDeduct;
            log.debug("Deducted {} from product {}, remaining to deduct: {}",
                    toDeduct, container.getProduct().getId(), remaining);
        }

        if (remaining > 0) {
            throw new InsufficientContainerException(
                    String.format("Failed to deduct all containers. Still need: %d", remaining)
            );
        }
    }

    private CustomerContainer createNewContainer(Long customerId, Long productId) {
        log.debug("Creating new container record: customerId={}, productId={}",
                customerId, productId);

        Customer customer = findCustomerById(customerId);
        Product product = findProductById(productId);

        CustomerContainer newContainer = new CustomerContainer();
        newContainer.setCustomer(customer);
        newContainer.setProduct(product);
        newContainer.setQuantity(0);
        newContainer.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        newContainer.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        return customerContainerRepository.save(newContainer);
    }

    private void validateInputs(Long customerId, Map<Long, Integer> productQuantities) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer id");
        }
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new IllegalArgumentException("Product quantities cannot be empty");
        }
    }

    private int getCustomerContainerBalance(Long customerId, Long productId) {
        return customerContainerRepository
                .findByCustomerIdAndProductId(customerId, productId)
                .map(CustomerContainer::getQuantity)
                .orElse(0);
    }

    private int getTotalCustomerContainerBalance(Long customerId) {
        return customerContainerRepository
                .findByCustomerId(customerId)
                .stream()
                .mapToInt(CustomerContainer::getQuantity)
                .sum();
    }

    private Integer getReservedContainerCount(Long customerId, Long productId) {
        Integer reserved = containerReservationRepository.sumReservedQuantity(
                customerId, productId, LocalDateTime.now()
        );
        return reserved != null ? reserved : 0;
    }

    private Customer findCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
    }

    private AffordablePackage findPackageById(Long packageId) {
        return affordablePackageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("Package not found with id: " + packageId));
    }
}