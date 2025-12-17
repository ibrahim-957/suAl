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

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderDetailFactory {
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final OrderCalculationService orderCalculationService;
    private final OrderDetailMapper orderDetailMapper;

    public OrderDetail createOrderDetail(OrderItemRequest item) {
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Price price = priceRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new RuntimeException("Price not found"));

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
}
