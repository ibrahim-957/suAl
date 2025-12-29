package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.mapper.OrderDetailMapper;
import com.delivery.SuAl.model.request.order.OrderItemRequest;
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

    public List<OrderDetail> createOrderDetails(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();

        List<Long> productIds = items.stream()
                .map(OrderItemRequest::getProductId)
                .distinct()
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        validateAllProductsExist(productIds, productMap);

        List<Price> prices = priceRepository.findAllByProductIdIn(productIds);
        Map<Long, Price> priceMap = prices.stream()
                .collect(Collectors.toMap(Price::getId, p -> p));

        validateAllPricesExist(productIds, priceMap);

        List<OrderDetail> orderDetails = new ArrayList<>();
        for (OrderItemRequest item : items) {
            Product product = productMap.get(item.getProductId());
            Price price = priceMap.get(item.getProductId());
            OrderDetail detail = buildOrderDetail(item, product, price);
            orderDetails.add(detail);
        }
        log.info("Created {} order details with batch loading", orderDetails.size());
        return orderDetails;
    }

    private OrderDetail buildOrderDetail(OrderItemRequest item, Product product, Price price) {
        OrderDetail orderDetail = orderDetailMapper.toEntity(item);
        orderDetail.setProduct(product);
        orderDetail.setCompany(product.getCompany());
        orderDetail.setCategory(product.getCategory());
        orderDetail.setPricePerUnit(price.getSellPrice());
        orderDetail.setBuyPrice(price.getBuyPrice());
        orderDetail.setDepositPerUnit(product.getDepositAmount());
        orderDetail.setContainersReturned(0);

        orderCalculationService.recalculateOrderDetail(orderDetail);
        return orderDetail;
    }

    private void validateAllProductsExist(List<Long> requestedIds, Map<Long, Product> productMap) {
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !productMap.containsKey(id))
                .collect(Collectors.toList());

        if (!missingIds.isEmpty()) {
            throw new RuntimeException("Product not found: " + missingIds);
        }
    }

    private void validateAllPricesExist(List<Long> requestedIds, Map<Long, Price> priceMap) {
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !priceMap.containsKey(id))
                .collect(Collectors.toList());

        if (!missingIds.isEmpty()) {
            throw new RuntimeException("Price not found: " + missingIds);
        }
    }
}