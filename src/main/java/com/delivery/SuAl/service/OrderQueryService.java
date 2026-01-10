package com.delivery.SuAl.service;

import com.delivery.SuAl.model.enums.OrderStatus;
import com.delivery.SuAl.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orderRepository;

    public int getCompletedOrderCount(Long userId){
        return orderRepository.countByUserIdAndOrderStatus(userId, OrderStatus.COMPLETED);
    }
}
