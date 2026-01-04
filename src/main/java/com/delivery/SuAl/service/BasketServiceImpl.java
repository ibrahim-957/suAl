package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Basket;
import com.delivery.SuAl.entity.BasketItem;
import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.InvalidRequestException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.helper.ProductDepositInfo;
import com.delivery.SuAl.mapper.BasketMapper;
import com.delivery.SuAl.model.ProductStatus;
import com.delivery.SuAl.model.request.basket.CreateOrderFromBasketRequest;
import com.delivery.SuAl.model.request.marketing.ValidatePromoRequest;
import com.delivery.SuAl.model.request.order.CreateOrderRequest;
import com.delivery.SuAl.model.request.order.OrderItemRequest;
import com.delivery.SuAl.model.response.basket.BasketCalculationResponse;
import com.delivery.SuAl.model.response.basket.BasketItemResponse;
import com.delivery.SuAl.model.response.basket.BasketResponse;
import com.delivery.SuAl.model.response.marketing.ValidatePromoResponse;
import com.delivery.SuAl.repository.BasketItemRepository;
import com.delivery.SuAl.repository.BasketRepository;
import com.delivery.SuAl.repository.PriceRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BasketServiceImpl implements BasketService {
    private final BasketRepository basketRepository;
    private final BasketItemRepository basketItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final ContainerManagementService containerManagementService;
    private final PromoService promoService;
    private final BasketMapper basketMapper;

    @Override
    public BasketResponse getOrCreateBasket(Long userId) {
        log.info("Getting or creating basket for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Basket basket = basketRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new basket for user: {}", userId);
                    Basket newBasket = new Basket();
                    newBasket.setUser(user);
                    return basketRepository.save(newBasket);
                });

        log.info("Basket retrieved/created successfully for user: {}, basketId: {}", userId, basket.getId());
        return basketMapper.toBasketResponse(basket);
    }

    @Override
    public BasketResponse getBasket(Long userId) {
        log.info("Fetching basket for user: {}", userId);

        Basket basket = basketRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new NotFoundException("Basket not found for user: " + userId));

        log.info("Basket found for user: {}, basketId: {}, items count: {}",
                userId, basket.getId(), basket.getBasketItems().size());
        return basketMapper.toBasketResponse(basket);
    }

    @Override
    public BasketResponse addItem(Long userId, Long productId, Integer quantity) {
        log.info("Adding item to basket - userId: {}, productId: {}, quantity: {}", userId, productId, quantity);

        Basket basket = basketRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    log.info("Basket not found, creating new  basket for user: {}", userId);
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
                    Basket newBasket = new Basket();
                    newBasket.setUser(user);
                    return basketRepository.save(newBasket);
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        if (product.getProductStatus() != ProductStatus.ACTIVE) {
            log.warn("Attempted to add inactive product: {} to basket for user: {}", productId, userId);
            throw new InvalidRequestException("Product is not active");
        }

        BasketItem existingItem = basketItemRepository.findByBasketIdAndProductId(basket.getId(), productId)
                .orElse(null);

        if (existingItem != null) {
            int oldQuantity = existingItem.getQuantity();
            int newQuantity = oldQuantity + quantity;
            existingItem.setQuantity(newQuantity);
            basketItemRepository.save(existingItem);
            log.info("Updated existing basket item - basketItemId: {}, oldQuantity: {}, newQuantity: {}",
                    existingItem.getId(), oldQuantity, newQuantity);
        } else {
            BasketItem newItem = new BasketItem();
            newItem.setBasket(basket);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            basketItemRepository.save(newItem);
            basket.addBasketItem(newItem);
            log.info("Created new basket item - basketItemId: {}, quantity: {}", newItem.getId(), quantity);
        }

        basket = basketRepository.save(basket);
        log.info("Item added successfully to basket: {}", basket.getId());

        basket = basketRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new NotFoundException("Basket not found after save"));
        return basketMapper.toBasketResponse(basket);
    }

    @Override
    public BasketResponse updateItem(Long userId, Long basketItemId, Integer quantity) {
        log.info("Updating basket item - userId: {}, basketItemId: {}, newQuantity: {}", userId, basketItemId, quantity);

        Basket basket = basketRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new NotFoundException("Basket not found for user: " + userId));

        BasketItem basketItem = basketItemRepository.findById(basketItemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + basketItemId));

        if (!basketItem.getBasket().getId().equals(basket.getId())) {
            log.warn("Attempted to update basket item {} that doesn't belong to user {}", basketItemId, userId);
            throw new InvalidRequestException("Basket item does not belong to user's basket");
        }

        if (quantity == 0) {
            log.info("Removing basket item {} due to zero quantity", basketItemId);
            basket.removeBasketItem(basketItem);
            basketItemRepository.delete(basketItem);
        } else {
            int oldQuantity = basketItem.getQuantity();
            basketItem.setQuantity(quantity);
            basketItemRepository.save(basketItem);
            log.info("Updated basket item {} quantity from {} to {}", basketItemId, oldQuantity, quantity);
        }

        basket = basketRepository.save(basket);
        log.info("Basket item updated successfully");

        basket = basketRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new NotFoundException("Basket not found after save"));
        return basketMapper.toBasketResponse(basket);
    }

    @Override
    public BasketResponse removeItem(Long userId, Long basketItemId) {
        log.info("Removing basket item - userId: {}, basketItemId: {}", userId, basketItemId);

        Basket basket = basketRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new NotFoundException("Basket not found for user: " + userId));

        BasketItem basketItem = basketItemRepository.findById(basketItemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + basketItemId));

        if (!basketItem.getBasket().getId().equals(basket.getId())) {
            log.warn("Attempted to remove basket item {} that doesn't belong to user {}", basketItemId, userId);
            throw new InvalidRequestException("Basket item does not belong to user's basket");
        }

        basket.removeBasketItem(basketItem);
        basketItemRepository.delete(basketItem);

        basket = basketRepository.save(basket);
        log.info("Basket item {} removed successfully from basket {}", basketItemId, basket.getId());

        basket = basketRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new NotFoundException("Basket not found after save"));
        return basketMapper.toBasketResponse(basket);
    }

    @Override
    public void clearBasket(Long userId) {
        log.info("Clearing basket for user: {}", userId);

        Basket basket = basketRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Basket not found for user: " + userId));

        int itemCount = basketItemRepository.countByBasketId(basket.getId());
        basketItemRepository.deleteByBasketId(basket.getId());
        log.info("Basket cleared successfully for user: {}, {} item removed", userId, itemCount);
    }

    @Override
    public BasketCalculationResponse calculateBasket(Long userId) {
        log.info("Calculating basket for user: {}", userId);

        Basket basket = basketRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new NotFoundException("Basket not found for user: " + userId));

        if (basket.getBasketItems().isEmpty()) {
            log.info("Basket is empty for user: {}, returning zero calculation", userId);
            return createEmptyCalculationResponse();
        }

        List<BasketItemResponse> itemResponses = basket.getBasketItems().stream()
                .map(this::mapBasketItemWithContainers)
                .toList();

        BigDecimal subtotal = itemResponses.stream()
                .map(BasketItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDepositCharged = itemResponses.stream()
                .map(item -> item.getDepositPerUnit()
                        .multiply(new BigDecimal(item.getQuantity() - item.getContainersToReturn())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDepositRefunded = itemResponses.stream()
                .map(item -> item.getDepositPerUnit()
                        .multiply(new BigDecimal(item.getContainersToReturn())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netDeposit = totalDepositCharged.subtract(totalDepositRefunded);
        BigDecimal amount = subtotal;
        BigDecimal totalAmount = amount.add(netDeposit);

        log.info("Basket calculation completed for user: {} - subtotal: {}, netDeposit: {}, totalAmount: {}",
                userId, subtotal, netDeposit, totalAmount);
        return BasketCalculationResponse.builder()
                .subtotal(subtotal)
                .totalDepositCharged(totalDepositCharged)
                .totalDepositRefunded(totalDepositRefunded)
                .netDeposit(netDeposit)
                .promoCode(null)
                .promoDiscount(BigDecimal.ZERO)
                .promoMessage(null)
                .campaignDiscount(BigDecimal.ZERO)
                .amount(amount)
                .totalAmount(totalAmount)
                .totalItems(basket.getBasketItems().size())
                .items(itemResponses)
                .build();
    }

    @Override
    public BasketCalculationResponse calculateBasketWithPromo(Long userId, String promoCode) {
        log.info("Calculating basket with promo for user: {}, promoCode: {}", userId, promoCode);

        BasketCalculationResponse calculation = calculateBasket(userId);

        if (promoCode != null && !promoCode.isBlank()) {
            try {
                log.info("Validating promo code: {} for user: {}", promoCode, userId);

                ValidatePromoRequest validateRequest = new ValidatePromoRequest();
                validateRequest.setPromoCode(promoCode);
                validateRequest.setOrderAmount(calculation.getSubtotal());
                validateRequest.setUserId(userId);

                ValidatePromoResponse promoValidation = promoService.validatePromo(validateRequest);

                if (Boolean.TRUE.equals(promoValidation.getIsValid()) &&
                        Boolean.TRUE.equals(promoValidation.getUserCanUse())) {
                    BigDecimal promoDiscount = promoValidation.getEstimatedDiscount();
                    BigDecimal newAmount = calculation.getSubtotal().subtract(promoDiscount);
                    BigDecimal newTotalAmount = newAmount.add(calculation.getNetDeposit());

                    calculation.setPromoCode(promoCode);
                    calculation.setPromoDiscount(promoDiscount);
                    calculation.setPromoValid(true);
                    calculation.setPromoMessage(promoValidation.getMessage());
                    calculation.setAmount(newAmount);
                    calculation.setTotalAmount(newTotalAmount);

                    log.info("Promo code applied successfully - discount: {}, newTotal: {}", promoDiscount, newTotalAmount);
                } else {
                    calculation.setPromoCode(promoCode);
                    calculation.setPromoDiscount(BigDecimal.ZERO);
                    calculation.setPromoValid(false);
                    calculation.setPromoMessage(promoValidation.getMessage());

                    log.warn("Promo code invalid: {} - message: {}", promoCode, promoValidation.getMessage());
                }
            } catch (Exception e) {
                log.error("Error validating promo code: {} for user: {}", promoCode, userId, e);

                calculation.setPromoCode(promoCode);
                calculation.setPromoDiscount(BigDecimal.ZERO);
                calculation.setPromoValid(false);
                calculation.setPromoMessage("Invalid promo code");
            }
        }

        return calculation;
    }

    @Override
    public CreateOrderRequest convertBasketToOrderRequest(Long userId, CreateOrderFromBasketRequest request) {
        log.info("Converting basket to order request for user: {}", userId);

        Basket basket = basketRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new NotFoundException("Basket not found for user: " + userId));

        if (basket.getBasketItems().isEmpty()){
            log.error("Cannot create order from empty basket for user: {}", userId);
            throw new InvalidRequestException("Cannot create order from empty basket");
        }

        List<OrderItemRequest> orderItems = basket.getBasketItems().stream()
                .map( item -> {
                    OrderItemRequest orderItem = new OrderItemRequest();
                    orderItem.setProductId(item.getProduct().getId());
                    orderItem.setQuantity(item.getQuantity());
                    return orderItem;
                })
                .toList();

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setAddressId(request.getAddressId());
        orderRequest.setDeliveryDate(request.getDeliveryDate());
        orderRequest.setItems(orderItems);
        orderRequest.setPromoCode(request.getPromoCode());
        orderRequest.setCampaignId(request.getCampaignId());
        orderRequest.setCampaignProductId(request.getCampaignProductId());
        orderRequest.setNotes(request.getNotes());

        log.info("Basket converted to order request - {} items, addressId: {}, deliveryDate: {}",
                orderItems.size(), request.getAddressId(), request.getDeliveryDate());

        return orderRequest;
    }

    private BasketItemResponse mapBasketItemWithContainers(BasketItem basketItem) {
        Price currentPrice = getCurrentPrice(basketItem.getProduct());

        Map<Long, Integer> productQuantities = Map.of(
                basketItem.getProduct().getId(),
                basketItem.getQuantity()
        );

        ContainerDepositSummary containerSummary = containerManagementService
                .calculateAvailableContainerRefunds(basketItem.getBasket().getUser().getId(), productQuantities);

        Integer availableContainers = containerSummary.getProductDepositInfoList().stream()
                .filter(info -> info.getProductId().equals(basketItem.getProduct().getId()))
                .findFirst()
                .map(ProductDepositInfo::getAvailableContainers)
                .orElse(0);

        Integer containersToReturn = Math.min(basketItem.getQuantity(), availableContainers);

        return basketMapper.toBasketItemResponseWithContainers(
                basketItem, currentPrice, availableContainers, containersToReturn
        );
    }

    private Price getCurrentPrice(Product product) {
        return priceRepository.findLatestByProductId(product.getId())
                .orElseThrow(() -> new NotFoundException("No price found for product: " + product.getId()));
    }

    private BasketCalculationResponse createEmptyCalculationResponse() {
        return BasketCalculationResponse.builder()
                .subtotal(BigDecimal.ZERO)
                .totalDepositCharged(BigDecimal.ZERO)
                .totalDepositRefunded(BigDecimal.ZERO)
                .netDeposit(BigDecimal.ZERO)
                .promoCode(null)
                .promoDiscount(BigDecimal.ZERO)
                .promoValid(false)
                .promoMessage(null)
                .campaignDiscount(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .totalItems(0)
                .items(List.of())
                .build();
    }
}