package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.UserContainer;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.model.request.order.BottleCollectionItem;

import java.util.List;
import java.util.Map;

public interface ContainerManagementService {
    ContainerDepositSummary calculateAvailableContainerRefunds(Long userId, Map<Long, Integer> productQuantities);

    void reserveContainers(Long userId, ContainerDepositSummary depositSummary);

    void releaseReservedContainers(Order order);

    void processCollectedBottles(Long userId, List<OrderDetail> orderDetails, List<BottleCollectionItem> bottlesCollected);

    UserContainer getOrCreateContainer(Long userId, Long productId);
}
