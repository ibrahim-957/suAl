package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderNumberGenerator {
    private final OrderRepository orderRepository;

    public synchronized String generateOrderNumber() {
        String prefix = "/" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<String> existingNumbers = orderRepository.findOrderNumbersByPrefix(prefix);

        int maxSequence = existingNumbers.stream()
                .map(this::extractSequence)
                .max(Integer::compareTo)
                .orElse(0);

        return String.format("%s-%04d", prefix, maxSequence + 1);
    }

    private int extractSequence(String orderNumber) {
        try {
            String numberPart = orderNumber.substring(orderNumber.lastIndexOf('-') + 1);
            return Integer.parseInt(numberPart);
        }catch (Exception e) {
            return 0;
        }
    }
}
