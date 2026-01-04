package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.Promo;
import com.delivery.SuAl.entity.PromoUsage;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.NotValidException;
import com.delivery.SuAl.exception.PromoUsageLimitExceededException;
import com.delivery.SuAl.mapper.PromoMapper;
import com.delivery.SuAl.model.PromoStatus;
import com.delivery.SuAl.model.request.marketing.ApplyPromoRequest;
import com.delivery.SuAl.model.request.marketing.CreatePromoRequest;
import com.delivery.SuAl.model.request.marketing.UpdatePromoRequest;
import com.delivery.SuAl.model.request.marketing.ValidatePromoRequest;
import com.delivery.SuAl.model.response.marketing.ApplyPromoResponse;
import com.delivery.SuAl.model.response.marketing.PromoResponse;
import com.delivery.SuAl.model.response.marketing.ValidatePromoResponse;
import com.delivery.SuAl.repository.PromoRepository;
import com.delivery.SuAl.repository.PromoUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PromoServiceImpl implements PromoService {
    private final PromoRepository promoRepository;
    private final PromoUsageRepository promoUsageRepository;
    private final PromoMapper promoMapper;

    @Override
    public PromoResponse createPromo(CreatePromoRequest request) {
        log.info("Creating promo with code: {}", request.getPromoCode());

        if (promoRepository.existsByPromoCode(request.getPromoCode()))
            throw new AlreadyExistsException("Promo already exists with code: " + request.getPromoCode());

        Promo promo = promoMapper.toEntity(request);
        Promo savedPromo = promoRepository.save(promo);

        log.info("Created promo with code: {}", savedPromo.getPromoCode());
        return promoMapper.toResponse(savedPromo);
    }

    @Override
    public PromoResponse getPromoById(Long id) {
        log.info("Getting promo with id: {}", id);

        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo not found with id: " + id));

        checkAndUpdateExpiredStatus(promo);
        return promoMapper.toResponse(promo);
    }

    @Override
    public Page<PromoResponse> getAllPromos(Pageable pageable) {
        log.info("Getting all promos with page: {}", pageable);

        Page<Promo> promos = promoRepository.findAll(pageable);

        promos.getContent().forEach(this::checkAndUpdateExpiredStatus);

        return promos.map(promoMapper::toResponse);
    }

    @Override
    public PromoResponse updatePromo(Long id, UpdatePromoRequest request) {
        log.info("Updating promo with id: {}", id);

        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo not found with id: " + id));

        LocalDate oldValidFrom = promo.getValidFrom();
        LocalDate oldValidTo = promo.getValidTo();

        promoMapper.updateEntityFromRequest(request, promo);

        if (request.getValidFrom() != null || request.getValidTo() != null) {
            LocalDate newValidFrom = request.getValidFrom() != null ? request.getValidFrom() : oldValidFrom;
            LocalDate newValidTo = request.getValidTo() != null ? request.getValidTo() : oldValidTo;

            if (promo.getPromoStatus() == PromoStatus.EXPIRED){
                LocalDate now = LocalDate.now();
                if (!now.isBefore(newValidFrom) && !now.isAfter(newValidTo)){
                    promo.setPromoStatus(PromoStatus.ACTIVE);
                    log.info("Promo {} reactivated due to valid date range", id);
                }
            }
        }

        Promo updatedPromo = promoRepository.save(promo);
        log.info("Updated promo with id: {}", id);

        return promoMapper.toResponse(updatedPromo);
    }

    @Override
    public void deletePromoById(Long id) {
        log.info("Soft deleting promo with id: {}", id);

        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo not found with id: " + id));

        promo.setPromoStatus(PromoStatus.INACTIVE);
        promoRepository.save(promo);

        log.info("Deleted promo with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidatePromoResponse validatePromo(ValidatePromoRequest request) {
        log.info("Validation promo code: {} for user: {}", request.getPromoCode(), request.getUserId());

        try{
            Promo promo = promoRepository.findByPromoCode(request.getPromoCode())
                    .orElseThrow(() -> new NotFoundException("Promo not found with code: " + request.getPromoCode()));

            validatePromoEligibility(promo, request.getUserId(), request.getOrderAmount());

            BigDecimal estimatedDiscount = promo.calculateDiscount(request.getOrderAmount());

            Integer userUsageCount = promoUsageRepository.countUsagesByUserAndPromo(
                    request.getUserId(), promo.getId());

            Boolean userCanUse = promo.getMaxUsesPerUser() == null || userUsageCount < promo.getMaxUsesPerUser();

            log.info("Promo validation successful. Estimated discount: {}", estimatedDiscount);

            return ValidatePromoResponse.builder()
                    .isValid(true)
                    .message("Promo code is valid")
                    .promoResponse(promoMapper.toResponse(promo))
                    .estimatedDiscount(estimatedDiscount)
                    .userCanUse(userCanUse)
                    .userUsageCount(userUsageCount)
                    .build();
        } catch (NotFoundException e) {
            log.warn("Promo not found: {}", e.getMessage());
            return ValidatePromoResponse.builder()
                    .isValid(false)
                    .message(e.getMessage())
                    .promoResponse(null)
                    .estimatedDiscount(BigDecimal.ZERO)
                    .userCanUse(false)
                    .userUsageCount(0)
                    .build();
        } catch (NotValidException | PromoUsageLimitExceededException e) {
            log.warn("Promo validation failed: {}", e.getMessage());
            return ValidatePromoResponse.builder()
                    .isValid(false)
                    .message(e.getMessage())
                    .promoResponse(null)
                    .estimatedDiscount(BigDecimal.ZERO)
                    .userCanUse(false)
                    .userUsageCount(0)
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("Promo calculation error: {}", e.getMessage());
            return ValidatePromoResponse.builder()
                    .isValid(false)
                    .message(e.getMessage())
                    .promoResponse(null)
                    .estimatedDiscount(BigDecimal.ZERO)
                    .userCanUse(false)
                    .userUsageCount(0)
                    .build();
        }
    }

    @Override
    public ApplyPromoResponse applyPromo(ApplyPromoRequest request) {
        log.info("Applying promo code: {} for user: {}", request.getPromoCode(), request.getUserId());

        Promo promo = promoRepository.findByPromoCode(request.getPromoCode())
                .orElseThrow(() -> new NotFoundException("Promo not found with code: " + request.getPromoCode()));

        validatePromoEligibility(promo, request.getUserId(), request.getOrderAmount());

        BigDecimal discountAmount;

        try{
            discountAmount = promo.calculateDiscount(request.getOrderAmount());
        }catch (IllegalArgumentException e){
            throw new NotValidException(e.getMessage());
        }

        PromoUsage promoUsage = new PromoUsage();

        User user = new User();
        user.setId(request.getUserId());
        promoUsage.setUser(user);

        if (request.getOrderId() != null) {
            Order order = new Order();
            order.setId(request.getOrderId());
            promoUsage.setOrder(order);
        }

        promoUsage.setPromo(promo);
        promoUsage.setDiscountApplied(discountAmount);

        PromoUsage savedUsage = promoUsageRepository.save(promoUsage);

        promo.incrementUses();
        promoRepository.save(promo);

        BigDecimal finalAmount = request.getOrderAmount().subtract(discountAmount);

        log.info("Promo applied successfully. Discount: {}, Final amount: {}", discountAmount, finalAmount);

        return ApplyPromoResponse.builder()
                .success(true)
                .message("Promo applied successfully")
                .promoUsageId(savedUsage.getId())
                .discountApplied(discountAmount)
                .originalAmount(request.getOrderAmount())
                .finalAmount(finalAmount)
                .promoCode(promo.getPromoCode())
                .build();
    }

    private void checkAndUpdateExpiredStatus(Promo promo) {
        if (promo.getPromoStatus() == PromoStatus.ACTIVE){
            LocalDate now = LocalDate.now();
            if (now.isAfter(promo.getValidTo())){
                promo.setPromoStatus(PromoStatus.EXPIRED);
                promoRepository.save(promo);
                log.info("Promo {} status changed to EXPIRED", promo.getPromoCode());
            }
        }
    }

    private void validatePromoEligibility(Promo promo, Long userId, BigDecimal orderAmount) {
        LocalDate now = LocalDate.now();

        if (promo.getPromoStatus() != PromoStatus.ACTIVE){
            throw new NotValidException("Promo status is not ACTIVE. Current status: " + promo.getPromoStatus());
        }


        if (now.isBefore(promo.getValidFrom())){
            throw new NotValidException("Promo is not yet valid. Valid from: " + promo.getValidFrom());
        }

        if (now.isAfter(promo.getValidTo())){
            throw new NotValidException("Promo has expired. Valid until: " + promo.getValidTo());
        }

        if (promo.hasReachedTotalLimit())
            throw new PromoUsageLimitExceededException("Promo has reached the maximum total limit.");


        if (promo.getMaxUsesPerUser() != null){
            Integer userUsageCount = promoUsageRepository.countUsagesByUserAndPromo(userId, promo.getId());
            if (userUsageCount >= promo.getMaxUsesPerUser()){
                throw new PromoUsageLimitExceededException(
                        String.format("You have already used this promo %d times. Maximum allowed: %d",
                                userUsageCount, promo.getMaxUsesPerUser())
                );
            }
        }

        if (orderAmount.compareTo(promo.getMinOrderAmount()) < 0){
            throw new NotValidException(
                    String.format("Order amount %.2f is below minimum required amount %.2f",
                            orderAmount, promo.getMinOrderAmount())
            );
        }
    }
}