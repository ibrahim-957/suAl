package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.ProductPrice;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.OrderDetailMapper;
import com.delivery.SuAl.model.request.cart.CartItem;
import com.delivery.SuAl.repository.ProductPriceRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderDetailFactory {
    private final ProductRepository productRepository;
    private final ProductPriceRepository priceRepository;
    private final OrderCalculationService orderCalculationService;
    private final OrderDetailMapper orderDetailMapper;

    public List<OrderDetail> createOrderDetailsFromCart(
            List<CartItem> cartItems,
            Map<Long, Integer> containersReturned
    ){
        if (cartItems == null || cartItems.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Creating order details from {} cart items", cartItems.size());

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        validateAllProductsExist(productIds, productMap);

        List<ProductPrice> activePrices = priceRepository.findActiveByProductIdIn(productIds);
        Map<Long, ProductPrice> priceMap = activePrices.stream()
                .collect(Collectors.toMap(
                        price -> price.getProduct().getId(),
                        p -> p
                ));
        validateAllPricesExist(productIds, priceMap);

        List<OrderDetail> orderDetails = new ArrayList<>();
        for(CartItem cartItem : cartItems){
            Product product = productMap.get(cartItem.getProductId());
            ProductPrice price = priceMap.get(cartItem.getProductId());
            int containers = containersReturned.getOrDefault(cartItem.getProductId(), 0);

            OrderDetail detail = buildOrderDetailFromCart(cartItem, product, price, containers);
            orderDetails.add(detail);
        }
        log.info("Created {} order details from cart with batch loading", orderDetails.size());
        return orderDetails;
    }

    private OrderDetail buildOrderDetailFromCart(
            CartItem cartItem,
            Product product,
            ProductPrice price,
            int containersReturned) {
        OrderDetail orderDetail = orderDetailMapper.toEntity(cartItem);
        orderDetail.setProduct(product);
        orderDetail.setCompany(product.getCompany());
        orderDetail.setCategory(product.getCategory());
        orderDetail.setCount(cartItem.getQuantity());

        BigDecimal sellPrice = calculateEffectivePrice(price);
        orderDetail.setPricePerUnit(sellPrice);

        orderDetail.setBuyPrice(price.getBuyPrice());
        orderDetail.setDepositPerUnit(product.getDepositAmount());
        orderDetail.setContainersReturned(containersReturned);

        orderCalculationService.recalculateOrderDetail(orderDetail);
        return orderDetail;
    }

    private BigDecimal calculateEffectivePrice(ProductPrice price){
        if (price.getSellPrice() == null) return BigDecimal.ZERO;
        if (price.getDiscountPercent() == null ||
        price.getDiscountPercent().compareTo(BigDecimal.ZERO) == 0)
            return price.getSellPrice();

        BigDecimal multiplier = BigDecimal.ONE
                .subtract(price.getDiscountPercent()
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return  price.getSellPrice()
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateAllProductsExist(List<Long> requestedIds, Map<Long, Product> productMap) {
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !productMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            log.error("Products not found: {}", missingIds);
            throw new NotFoundException(
                    String.format("Products not found. Missing %d product(s). First few IDs: %s",
                            missingIds.size(),
                            missingIds.stream().limit(3).map(String::valueOf).collect(Collectors.joining(", ")))
            );
        }
    }

    private void validateAllPricesExist(List<Long> requestedIds, Map<Long, ProductPrice> priceMap) {
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !priceMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new NotFoundException(
                    String.format("Products not found. Missing %d product(s): %s",
                            missingIds.size(),
                            missingIds.stream().limit(5).map(String::valueOf).collect(Collectors.joining(", ")))
            );
        }
    }
}