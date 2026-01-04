package com.delivery.SuAl.mapper;

import com.delivery.SuAl.entity.Basket;
import com.delivery.SuAl.entity.BasketItem;
import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.model.response.basket.BasketItemResponse;
import com.delivery.SuAl.model.response.basket.BasketResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BasketMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "items", expression = "java(mapBasketItemsToResponses(basket))")
    @Mapping(target = "totalItems", expression = "java(calculateTotalItems(basket))")
    BasketResponse toBasketResponse(Basket basket);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "basketItem.id", target = "id")
    @Mapping(source = "basketItem.product.id", target = "productId")
    @Mapping(source = "basketItem.product.name", target = "productName")
    @Mapping(source = "basketItem.product.size", target = "productSize")
    @Mapping(source = "basketItem.product.company.name", target = "companyName")
    @Mapping(source = "basketItem.quantity", target = "quantity")
    @Mapping(source = "currentPrice.sellPrice", target = "pricePerUnit")
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(basketItem, currentPrice))")
    @Mapping(source = "basketItem.product.depositAmount", target = "depositPerUnit")
    @Mapping(target = "availableContainers", constant = "0")
    @Mapping(target = "containersToReturn", constant = "0")
    BasketItemResponse toBasketItemResponse(BasketItem basketItem, Price currentPrice);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "basketItem.id", target = "id")
    @Mapping(source = "basketItem.product.id", target = "productId")
    @Mapping(source = "basketItem.product.name", target = "productName")
    @Mapping(source = "basketItem.product.size", target = "productSize")
    @Mapping(source = "basketItem.product.company.name", target = "companyName")
    @Mapping(source = "basketItem.quantity", target = "quantity")
    @Mapping(source = "currentPrice.sellPrice", target = "pricePerUnit")
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(basketItem, currentPrice))")
    @Mapping(source = "basketItem.product.depositAmount", target = "depositPerUnit")
    @Mapping(source = "availableContainers", target = "availableContainers")
    @Mapping(source = "containersToReturn", target = "containersToReturn")
    BasketItemResponse toBasketItemResponseWithContainers(
            BasketItem basketItem,
            Price currentPrice,
            Integer availableContainers,
            Integer containersToReturn
    );

    default Integer calculateTotalItems(Basket basket) {
        if (basket == null || basket.getBasketItems() == null) {
            return 0;
        }
        return basket.getBasketItems().stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
    }

    default List<BasketItemResponse> mapBasketItemsToResponses(Basket basket) {
        if (basket == null || basket.getBasketItems() == null) {
            return List.of();
        }

        return basket.getBasketItems().stream()
                .map(basketItem -> {
                    Price currentPrice = getCurrentPriceForItem(basketItem);
                    return toBasketItemResponse(basketItem, currentPrice);
                })
                .collect(Collectors.toList());
    }

    default Price getCurrentPriceForItem(BasketItem basketItem) {
        if (basketItem == null || basketItem.getProduct() == null) {
            throw new RuntimeException("BasketItem or Product is null");
        }

        if (basketItem.getProduct().getPrices() == null || basketItem.getProduct().getPrices().isEmpty()) {
            throw new RuntimeException("No price found for product: " + basketItem.getProduct().getId());
        }

        List<Price> prices = basketItem.getProduct().getPrices();
        return prices.get(prices.size() - 1);
    }

    default BigDecimal calculateSubtotal(BasketItem basketItem, Price currentPrice) {
        if (basketItem == null || basketItem.getQuantity() == null ||
                currentPrice == null || currentPrice.getSellPrice() == null) {
            return BigDecimal.ZERO;
        }
        return currentPrice.getSellPrice()
                .multiply(new BigDecimal(basketItem.getQuantity()));
    }

    default Integer calculateContainersToReturn(Integer quantity, Integer availableContainers) {
        if (quantity == null || availableContainers == null) {
            return 0;
        }
        return Math.min(quantity, availableContainers);
    }
}