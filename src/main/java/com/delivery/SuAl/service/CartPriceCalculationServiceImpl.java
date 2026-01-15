package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.exception.InvalidRequestException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.helper.ContainerDepositSummary;
import com.delivery.SuAl.helper.ProductDepositInfo;
import com.delivery.SuAl.model.enums.ProductStatus;
import com.delivery.SuAl.model.request.cart.CalculatePriceRequest;
import com.delivery.SuAl.model.request.cart.CartItem;
import com.delivery.SuAl.model.request.marketing.GetEligibleCampaignsRequest;
import com.delivery.SuAl.model.request.marketing.ValidatePromoRequest;
import com.delivery.SuAl.model.response.cart.CartCalculationResponse;
import com.delivery.SuAl.model.response.cart.CartItemResponse;
import com.delivery.SuAl.model.response.marketing.EligibleCampaignsResponse;
import com.delivery.SuAl.model.response.marketing.ValidatePromoResponse;
import com.delivery.SuAl.repository.PriceRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartPriceCalculationServiceImpl implements CartPriceCalculationService {
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final ContainerManagementService containerManagementService;
    private final PromoService promoService;
    private final CampaignService campaignService;

    @Override
    public CartCalculationResponse calculatePrice(CalculatePriceRequest request) {
        log.info("Calculating price for userId: {}, {} items",
                request.getUserId(), request.getItems().size());

        if (request.getItems().isEmpty()) {
            return createEmptyResponse();
        }

        List<CartItemResponse> itemResponses = request.getItems().stream()
                .map(item -> mapCartItemToResponse(request.getUserId(), item))
                .toList();

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDepositCharged = itemResponses.stream()
                .map(item -> item.getDepositPerUnit()
                        .multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDepositRefunded = itemResponses.stream()
                .map(item -> item.getDepositPerUnit()
                        .multiply(new BigDecimal(item.getAvailableContainers())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netDeposit = totalDepositCharged.subtract(totalDepositRefunded);

        Map<Long, Integer> productQuantities = request.getItems().stream()
                .collect(Collectors.toMap(
                        CartItem::getProductId,
                        CartItem::getQuantity
                ));

        boolean willUsePromo = (request.getPromoCode() != null && !request.getPromoCode().isBlank());

        GetEligibleCampaignsRequest campaignsRequest = GetEligibleCampaignsRequest.builder()
                .userId(request.getUserId())
                .productQuantities(productQuantities)
                .willUsePromoCode(willUsePromo)
                .build();

        EligibleCampaignsResponse eligibleCampaigns = campaignService.getEligibleCampaigns(campaignsRequest);
        BigDecimal campaignDiscount = eligibleCampaigns.getTotalCampaignDiscount();

        BigDecimal amount = subtotal;
        BigDecimal totalAmount = amount.add(netDeposit);

        CartCalculationResponse.CartCalculationResponseBuilder responseBuilder = CartCalculationResponse.builder()
                .subtotal(subtotal)
                .totalDepositCharged(totalDepositCharged)
                .totalDepositRefunded(totalDepositRefunded)
                .netDeposit(netDeposit)
                .campaignDiscount(campaignDiscount)
                .eligibleCampaigns(eligibleCampaigns)
                .totalItems(request.getItems().size())
                .items(itemResponses);

        if (willUsePromo) {
            log.info("Validating promo code: {} for user: {}", request.getPromoCode(), request.getUserId());

            ValidatePromoRequest validateRequest = new ValidatePromoRequest();
            validateRequest.setPromoCode(request.getPromoCode());
            validateRequest.setUserId(request.getUserId());
            validateRequest.setOrderAmount(subtotal);

            ValidatePromoResponse promoValidation = promoService.validatePromo(validateRequest);

            if (Boolean.TRUE.equals(promoValidation.getIsValid()) && Boolean.TRUE.equals(promoValidation.getUserCanUse())) {
                BigDecimal promoDiscount = promoValidation.getEstimatedDiscount();
                BigDecimal newAmount = subtotal.subtract(promoDiscount);
                BigDecimal newTotalAmount = newAmount.add(netDeposit);

                responseBuilder
                        .promoCode(request.getPromoCode())
                        .promoDiscount(promoDiscount)
                        .promoValid(true)
                        .promoMessage(promoValidation.getMessage())
                        .amount(newAmount)
                        .totalAmount(newTotalAmount);

                log.info("Promo applied - discount: {}, newTotal: {}", promoDiscount, newTotalAmount);
            } else {
                responseBuilder
                        .promoCode(request.getPromoCode())
                        .promoDiscount(BigDecimal.ZERO)
                        .promoValid(false)
                        .amount(amount)
                        .totalAmount(totalAmount);

                log.warn("Promo code invalid: {} - message: {}", request.getPromoCode(), promoValidation.getMessage());
            }

        } else {
            responseBuilder
                    .promoCode(request.getPromoCode())
                    .promoDiscount(BigDecimal.ZERO)
                    .amount(amount)
                    .totalAmount(totalAmount);
        }
        return responseBuilder.build();
    }

    private CartItemResponse mapCartItemToResponse(Long userId, CartItem cartItem) {
        Product product = productRepository.findById(cartItem.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + cartItem.getProductId()));

        if (product.getProductStatus() != ProductStatus.ACTIVE) {
            throw new InvalidRequestException("Product is not active: " + cartItem.getProductId());
        }

        Price currentPrice = priceRepository.findFirstByProduct_IdOrderByCreatedAtDesc(product.getId())
                .orElseThrow(() -> new NotFoundException("Price not found for product with id: " + product.getId()));

        Map<Long, Integer> productQuantities = Map.of(product.getId(), cartItem.getQuantity());

        ContainerDepositSummary containerSummary =
                containerManagementService.calculateAvailableContainerRefunds(userId, productQuantities);

        Integer availableContainers = containerSummary.getProductDepositInfos().stream()
                .filter(info -> info.getProductId().equals(product.getId()))
                .findFirst()
                .map(ProductDepositInfo::getAvailableContainers)
                .orElse(0);

        Integer containersTOReturn = Math.min(cartItem.getQuantity(), availableContainers);

        BigDecimal subtotal = currentPrice.getSellPrice()
                .multiply(new BigDecimal(cartItem.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        return CartItemResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .quantity(cartItem.getQuantity())
                .pricePerUnit(currentPrice.getSellPrice())
                .subtotal(subtotal)
                .depositPerUnit(product.getDepositAmount())
                .availableContainers(availableContainers)
                .containersToReturn(containersTOReturn)
                .build();
    }

    private CartCalculationResponse createEmptyResponse() {
        return CartCalculationResponse.builder()
                .subtotal(BigDecimal.ZERO)
                .totalDepositCharged(BigDecimal.ZERO)
                .totalDepositRefunded(BigDecimal.ZERO)
                .netDeposit(BigDecimal.ZERO)
                .promoCode(null)
                .promoDiscount(BigDecimal.ZERO)
                .promoValid(false)
                .promoMessage(null)
                .campaignDiscount(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .totalItems(0)
                .items(List.of())
                .build();
    }
}