package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.entity.UserContainer;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.helper.ProductDepositInfo;
import com.delivery.SuAl.model.request.order.BottleCollectionItem;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.UserContainerRepository;
import com.delivery.SuAl.repository.UserRepository;
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
    private final UserContainerRepository userContainerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public ContainerDepositSummary calculateAvailableContainerRefunds(Long userId, Map<Long, Integer> productQuantities) {
        log.info("Calculating container refunds for user {}", userId);

        List<ProductDepositInfo> productDepositInfos = new ArrayList<>();
        int totalContainersUsed = 0;
        BigDecimal totalDepositRefunded = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer orderQuantity = entry.getValue();

            UserContainer userContainer = userContainerRepository
                    .findByUserIdAndProductId(userId, productId)
                    .orElse(null);

            int availableContainers = (userContainer != null) ? userContainer.getQuantity() : 0;

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

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
    public void reserveContainers(Long userId, ContainerDepositSummary depositSummary) {
        log.info("Reserving containers for user {}", userId);
        for (ProductDepositInfo info : depositSummary.getProductDepositInfos()) {
            if (info.getContainersUsed() > 0) {
                UserContainer container = userContainerRepository
                        .findByUserIdAndProductId(userId, info.getProductId())
                        .orElseThrow(() -> new RuntimeException("Container not found with id: " + info.getProductId()));

                int newQuantity = container.getQuantity() - info.getContainersUsed();

                if (newQuantity < 0) {
                    throw new RuntimeException("New container quantity is less than 0");
                }

                container.setQuantity(newQuantity);
                userContainerRepository.save(container);

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
                UserContainer container = getOrCreateContainer(
                        order.getUser().getId(),
                        detail.getProduct().getId()
                );

                container.setQuantity(container.getQuantity() + detail.getContainersReturned());
                userContainerRepository.save(container);

                log.debug("Released {} containers for product {}",
                        detail.getContainersReturned(), detail.getProduct().getId());
            }
        }
    }

    @Override
    @Transactional
    public void processCollectedBottles(
            Long userId,
            List<OrderDetail> orderDetails,
            List<BottleCollectionItem> bottlesCollected) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (bottlesCollected == null || bottlesCollected.isEmpty()) {
            log.info("No bottles collected for user {}", userId);
            for (OrderDetail detail : orderDetails) {
                detail.setContainersReturned(0);
            }
            return;
        }

        log.info("Processing {} collected bottles for user {}", bottlesCollected.size(), userId);

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
                removeFromUserContainer(user, detail.getProduct(), actualCollected);
            } else {
                log.debug("User {}: No containers collected for product {}", userId, productId);
            }
        }

        userRepository.save(user);

        Map<Long, Integer> bottlesToAddToWarehouse = collectedByProduct.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!bottlesToAddToWarehouse.isEmpty()) {
            inventoryService.addEmptyBottlesBatch(bottlesToAddToWarehouse);
        }

        int totalCollected = bottlesCollected.stream()
                .mapToInt(BottleCollectionItem::getQuantity)
                .sum();

        log.info("Completed bottle collection for user {}: {} bottles collected across {} products",
                userId, totalCollected, collectedByProduct.size());
    }

    @Override
    @Transactional
    public void processDeliveredProducts(Long userId, List<OrderDetail> orderDetails) {
        log.info("Processing delivered products for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        int totalDelivered = 0;

        for (OrderDetail detail : orderDetails) {
            int deliveredQuantity = detail.getCount();

            if (deliveredQuantity > 0) {
                addOrUpdateUserContainer(user, detail.getProduct(), deliveredQuantity);
                totalDelivered += deliveredQuantity;

                log.debug("User {}: Added {} containers of product {} ({})",
                        userId, deliveredQuantity,
                        detail.getProduct().getId(),
                        detail.getProduct().getName());
            }
        }

        userRepository.save(user);

        log.info("Completed delivery processing for user {}: {} bottles delivered across {} products",
                userId, totalDelivered, orderDetails.size());
    }

    private void removeFromUserContainer(User user, Product product, Integer quantity) {
        UserContainer existing = user.getUserContainers().stream()
                .filter(uc -> uc.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            int previousQuantity = existing.getQuantity();
            int newQuantity = previousQuantity - quantity;

            if (newQuantity < 0) {
                log.warn("User {}: Trying to remove {} containers for product {}, but only {} available. Setting to 0.",
                        user.getId(), quantity, product.getId(), previousQuantity);
                newQuantity = 0;
            }

            existing.setQuantity(newQuantity);
            log.debug("User {}: Removed {} containers for product {}. Previous: {}, New: {}",
                    user.getId(), quantity, product.getId(), previousQuantity, newQuantity);

            if (newQuantity == 0) {
                user.getUserContainers().remove(existing);
                log.debug("User {}: Removed empty container record for product {}",
                        user.getId(), product.getId());
            }
        } else {
            log.warn("User {}: Cannot remove containers for product {} - no container record exists",
                    user.getId(), product.getId());
        }
    }

    private void addOrUpdateUserContainer(User user, Product product, Integer quantity) {
        Optional<UserContainer> existing = userContainerRepository
                .findByUserIdAndProductId(user.getId(), product.getId());

        if (existing.isPresent()) {
            UserContainer container = existing.get();
            int previousQuantity = container.getQuantity();
            container.setQuantity(previousQuantity + quantity);
            container.setUpdatedAt(LocalDateTime.now());
            userContainerRepository.save(container);

            log.debug("User {}: Updated container for product {}. Previous: {}, New: {}",
                    user.getId(), product.getId(), previousQuantity, container.getQuantity());
        } else {
            UserContainer newContainer = new UserContainer();
            newContainer.setUser(user);
            newContainer.setProduct(product);
            newContainer.setQuantity(quantity);
            newContainer.setCreatedAt(LocalDateTime.now());
            newContainer.setUpdatedAt(LocalDateTime.now());
            userContainerRepository.save(newContainer);

            log.debug("User {}: Created new container for product {} with quantity {}",
                    user.getId(), product.getId(), quantity);
        }
    }

    @Override
    @Transactional
    public UserContainer getOrCreateContainer(Long userId, Long productId) {
        return userContainerRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> {
                    log.debug("Creating new container record for user {} and product {}",
                            userId, productId);

                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

                    UserContainer newContainer = new UserContainer();
                    newContainer.setUser(user);
                    newContainer.setProduct(product);
                    newContainer.setQuantity(0);

                    return userContainerRepository.save(newContainer);
                });
    }
}