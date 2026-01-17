package com.delivery.SuAl.service;

import com.delivery.SuAl.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrderNumberGenerator {
    private final OrderRepository orderRepository;

    public String generateOrderNumber() {
        String datePrefix = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Long sequence = orderRepository.getNextOrderSequence();
        return String.format("ORD-%s-%04d", datePrefix, sequence);
    }
}