package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.OrderDetailMapper;
import com.delivery.SuAl.model.request.cart.CartItem;
import com.delivery.SuAl.repository.PriceRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final PriceRepository priceRepository;
    private final OrderCalculationService orderCalculationService;
    private final OrderDetailMapper orderDetailMapper;

    public List<OrderDetail> createOrderDetailsFromCart(
            List<CartItem> cartItems,
            Map<Long, Integer> containersReturned
    ){
        if (cartItems == null || cartItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        validateAllProductsExist(productIds, productMap);

        List<Price> prices = priceRepository.findAllByProductIdIn(productIds);
        Map<Long, Price> priceMap = prices.stream()
                .collect(Collectors.toMap(
                        price -> price.getProduct().getId(),
                        p -> p
                ));

        validateAllPricesExist(productIds, priceMap);

        List<OrderDetail> orderDetails = new ArrayList<>();
        for(CartItem cartItem : cartItems){
            Product product = productMap.get(cartItem.getProductId());
            Price price = priceMap.get(cartItem.getProductId());
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
            Price price,
            int containersReturned) {
        OrderDetail orderDetail = orderDetailMapper.toEntity(cartItem);
        orderDetail.setProduct(product);
        orderDetail.setCompany(product.getCompany());
        orderDetail.setCategory(product.getCategory());
        orderDetail.setCount(cartItem.getQuantity());
        orderDetail.setPricePerUnit(price.getSellPrice());
        orderDetail.setBuyPrice(price.getBuyPrice());
        orderDetail.setDepositPerUnit(product.getDepositAmount());
        orderDetail.setContainersReturned(containersReturned);

        orderCalculationService.recalculateOrderDetail(orderDetail);
        return orderDetail;
    }

    private void validateAllProductsExist(List<Long> requestedIds, Map<Long, Product> productMap) {
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !productMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Product not found: " + missingIds);
        }
    }

    private void validateAllPricesExist(List<Long> requestedIds, Map<Long, Price> priceMap) {
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !priceMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Price not found for products: " + missingIds);
        }
    }
}