package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.CustomerPackageOrder;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.PackageDeliveryDistribution;
import com.delivery.SuAl.entity.PackageDeliveryItem;
import com.delivery.SuAl.model.response.affordablepackage.CustomerPackageOrderResponse;
import com.delivery.SuAl.model.response.affordablepackage.DeliveryDistributionResponse;
import com.delivery.SuAl.model.response.affordablepackage.DeliveryProductResponse;
import com.delivery.SuAl.model.response.affordablepackage.GeneratedOrderSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerPackageOrderMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(getCustomerName(packageOrder))")
    @Mapping(target = "packageId", source = "affordablePackage.id")
    @Mapping(target = "packageName", source = "affordablePackage.name")
    @Mapping(target = "deliveryDistributions", expression = "java(mapDistributions(packageOrder.getDeliveryDistributions()))")
    @Mapping(target = "generatedOrders", expression = "java(mapGeneratedOrders(packageOrder.getGeneratedOrders()))")
    @Mapping(target = "createdAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "updatedAt", qualifiedByName = "utcToBaku")
    @Mapping(target = "cancelledAt", qualifiedByName = "utcToBaku")
    CustomerPackageOrderResponse toResponse(CustomerPackageOrder packageOrder);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "deliveryNumber", source = "deliveryNumber")
    @Mapping(target = "deliveryDate", source = "deliveryDate")
    @Mapping(target = "addressId", source = "address.id")
    @Mapping(target = "addressFullAddress", source = "address.fullAddress")
    @Mapping(target = "products", expression = "java(mapDeliveryItems(distribution.getDeliveryItems()))")
    @Mapping(target = "totalQuantity", expression = "java(distribution.getTotalQuantity())")
    DeliveryDistributionResponse toDistributionResponse(PackageDeliveryDistribution distribution);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "quantity", source = "quantity")
    DeliveryProductResponse toDeliveryProductResponse(PackageDeliveryItem item);

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "orderNumber", source = "orderNumber")
    @Mapping(target = "deliveryNumber", source = "deliveryNumber")
    @Mapping(target = "orderStatus", expression = "java(order.getOrderStatus().name())")
    @Mapping(target = "deliveryDate", source = "deliveryDate")
    @Mapping(target = "totalAmount", source = "totalAmount")
    GeneratedOrderSummary toGeneratedOrderSummary(Order order);

    default List<DeliveryDistributionResponse> mapDistributions(
            List<PackageDeliveryDistribution> distributions) {
        if (distributions == null) {
            return null;
        }
        return distributions.stream()
                .map(this::toDistributionResponse)
                .collect(Collectors.toList());
    }

    default List<DeliveryProductResponse> mapDeliveryItems(
            List<PackageDeliveryItem> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(this::toDeliveryProductResponse)
                .collect(Collectors.toList());
    }

    default List<GeneratedOrderSummary> mapGeneratedOrders(
            List<Order> orders) {
        if (orders == null) {
            return null;
        }
        return orders.stream()
                .map(this::toGeneratedOrderSummary)
                .collect(Collectors.toList());
    }

    default String getCustomerName(CustomerPackageOrder packageOrder) {
        if (packageOrder.getCustomer() == null) {
            return null;
        }
        String firstName = packageOrder.getCustomer().getFirstName() != null
                ? packageOrder.getCustomer().getFirstName() : "";
        String lastName = packageOrder.getCustomer().getLastName() != null
                ? packageOrder.getCustomer().getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? null : fullName;
    }
}
