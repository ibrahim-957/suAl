package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.CustomerContainer;
import com.delivery.SuAl.entity.CustomerPackageOrder;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.model.request.order.BottleCollectionItem;

import java.util.List;
import java.util.Map;

public interface ContainerManagementService {
    ContainerDepositSummary calculateAvailableContainerRefunds(Long userId, Map<Long, Integer> productQuantities);
    void processOrderCompletion(Long customerId, List<OrderDetail> orderDetails, List<BottleCollectionItem> bottlesCollected);


    CustomerContainer getOrCreateContainer(Long userId, Long productId);

    void processPackageOrderCompletion(CustomerPackageOrder packageOrder);

    boolean validatePackageContainerAvailability(Long customerId, Long packageId, int quantity);

    void reserveContainersForOrder(Order order, ContainerDepositSummary depositSummary);

    void releaseContainerReservations(Long orderId);


}
