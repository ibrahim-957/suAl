package com.delivery.SuAl.controller;

import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.InvalidRequestException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.model.OperatorStatus;
import com.delivery.SuAl.model.request.basket.AddToBasketByOperatorRequest;
import com.delivery.SuAl.model.request.basket.AddToBasketRequest;
import com.delivery.SuAl.model.request.basket.UpdateBasketItemRequest;
import com.delivery.SuAl.model.request.marketing.PreviewPromoRequest;
import com.delivery.SuAl.model.response.basket.BasketCalculationResponse;
import com.delivery.SuAl.model.response.basket.BasketResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.repository.OperatorRepository;
import com.delivery.SuAl.repository.UserRepository;
import com.delivery.SuAl.service.BasketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/baskets")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BasketController {
    private final BasketService basketService;
    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;

    @GetMapping("/my-basket")
    public ResponseEntity<ApiResponse<BasketResponse>> getMyBasket(
            @RequestHeader("X-User_Phone") String phoneNumber
    ) {
        log.info("Getting basket for user with phone: {}", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found with phone number: " + phoneNumber));

        BasketResponse response = basketService.getBasket(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Basket retrieved", response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<BasketResponse>> addItem(
            @RequestHeader("X-User_Phone") String phoneNumber,
            @Valid @RequestBody AddToBasketRequest request
            ){
        log.info("Adding item to basket for user with phone: {}", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found with phone number: " + phoneNumber));

        BasketResponse response = basketService.addItem(user.getId(), request.getProductId(), request.getQuantity());

        return ResponseEntity.ok(ApiResponse.success("Item added to basket", response));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<BasketResponse>> updateItem(
            @RequestHeader("X-User_Phone") String phoneNumber,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateBasketItemRequest request
    ){
        log.info("Updating basket item {} for user with phone: {}", itemId, phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found with phone number: " + phoneNumber));

        BasketResponse response = basketService.updateItem(user.getId(), itemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Basket item updated", response));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<BasketResponse>> removeItem(
            @RequestHeader("X-User-Phone") String phoneNumber,
            @PathVariable Long itemId
    ) {
        log.info("Removing basket item {} for user with phone: {}", itemId, phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found"));

        BasketResponse response = basketService.removeItem(user.getId(), itemId);

        return ResponseEntity.ok(ApiResponse.success("Item removed from basket", response));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearBasket(
            @RequestHeader("X-User-Phone") String phoneNumber
    ) {
        log.info("Clearing basket for user with phone: {}", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found"));

        basketService.clearBasket(user.getId());

        return ResponseEntity.ok(ApiResponse.success("Basket cleared", null));
    }

    @GetMapping("/calculate")
    public ResponseEntity<ApiResponse<BasketCalculationResponse>> calculateBasket(
            @RequestHeader("X-User-Phone") String phoneNumber
    ) {
        log.info("Calculating basket for user with phone: {}", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found"));

        BasketCalculationResponse response = basketService.calculateBasket(user.getId());

        return ResponseEntity.ok(ApiResponse.success("Basket calculated", response));
    }

    @PostMapping("/preview-promo")
    public ResponseEntity<ApiResponse<BasketCalculationResponse>> previewPromo(
            @RequestHeader("X-User-Phone") String phoneNumber,
            @Valid @RequestBody PreviewPromoRequest request
    ) {
        log.info("Previewing promo code for user with phone: {}", phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("User not found"));

        BasketCalculationResponse response = basketService.calculateBasketWithPromo(user.getId(), request.getPromoCode());

        return ResponseEntity.ok(ApiResponse.success("Promo preview calculated", response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<BasketResponse>> getBasketByOperator(
            @RequestHeader("X-Operator-Email") String operatorEmail,
            @PathVariable Long userId
    ) {
        log.info("Operator {} getting basket for user: {}", operatorEmail, userId);

        validateOperator(operatorEmail);

        BasketResponse response = basketService.getBasket(userId);

        return ResponseEntity.ok(ApiResponse.success("Basket retrieved", response));
    }

    @PostMapping("/items/by-operator")
    public ResponseEntity<ApiResponse<BasketResponse>> addItemByOperator(
            @RequestHeader("X-Operator-Email") String operatorEmail,
            @Valid @RequestBody AddToBasketByOperatorRequest request
    ) {
        log.info("Operator {} adding item to basket for user: {}", operatorEmail, request.getUserId());

        validateOperator(operatorEmail);

        BasketResponse response = basketService.addItem(request.getUserId(), request.getProductId(), request.getQuantity());

        return ResponseEntity.ok(ApiResponse.success("Item added to basket", response));
    }

    @PutMapping("/items/{itemId}/by-operator")
    public ResponseEntity<ApiResponse<BasketResponse>> updateItemByOperator(
            @RequestHeader("X-Operator-Email") String operatorEmail,
            @PathVariable Long itemId,
            @RequestParam Long userId,
            @Valid @RequestBody UpdateBasketItemRequest request
    ) {
        log.info("Operator {} updating basket item {} for user: {}", operatorEmail, itemId, userId);

        validateOperator(operatorEmail);

        BasketResponse response = basketService.updateItem(userId, itemId, request.getQuantity());

        return ResponseEntity.ok(ApiResponse.success("Basket item updated", response));
    }

    @DeleteMapping("/items/{itemId}/by-operator")
    public ResponseEntity<ApiResponse<BasketResponse>> removeItemByOperator(
            @RequestHeader("X-Operator-Email") String operatorEmail,
            @PathVariable Long itemId,
            @RequestParam Long userId
    ) {
        log.info("Operator {} removing basket item {} for user: {}", operatorEmail, itemId, userId);

        validateOperator(operatorEmail);

        BasketResponse response = basketService.removeItem(userId, itemId);

        return ResponseEntity.ok(ApiResponse.success("Item removed from basket", response));
    }

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearBasketByOperator(
            @RequestHeader("X-Operator-Email") String operatorEmail,
            @PathVariable Long userId
    ) {
        log.info("Operator {} clearing basket for user: {}", operatorEmail, userId);

        validateOperator(operatorEmail);

        basketService.clearBasket(userId);

        return ResponseEntity.ok(ApiResponse.success("Basket cleared", null));
    }

    @GetMapping("/user/{userId}/calculate")
    public ResponseEntity<ApiResponse<BasketCalculationResponse>> calculateBasketByOperator(
            @RequestHeader("X-Operator-Email") String operatorEmail,
            @PathVariable Long userId
    ) {
        log.info("Operator {} calculating basket for user: {}", operatorEmail, userId);

        validateOperator(operatorEmail);

        BasketCalculationResponse response = basketService.calculateBasket(userId);

        return ResponseEntity.ok(ApiResponse.success("Basket calculated", response));
    }

    @PostMapping("/user/{userId}/preview-promo")
    public ResponseEntity<ApiResponse<BasketCalculationResponse>> previewPromoByOperator(
            @RequestHeader("X-Operator-Email") String operatorEmail,
            @PathVariable Long userId,
            @Valid @RequestBody PreviewPromoRequest request
    ) {
        log.info("Operator {} previewing promo for user: {}", operatorEmail, userId);

        validateOperator(operatorEmail);

        BasketCalculationResponse response = basketService.calculateBasketWithPromo(userId, request.getPromoCode());

        return ResponseEntity.ok(ApiResponse.success("Promo preview calculated", response));
    }

    private void validateOperator(String operatorEmail){
        Operator operator = operatorRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new NotFoundException("Operator not found with email: " + operatorEmail));

        if (operator.getOperatorStatus() != OperatorStatus.ACTIVE){
            throw new InvalidRequestException("Operator is not active.");
        }
    }
}