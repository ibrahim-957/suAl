package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.Promo;
import com.delivery.SuAl.entity.PromoUsage;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.NotValidException;
import com.delivery.SuAl.exception.PromoUsageLimitExceededException;
import com.delivery.SuAl.mapper.PromoMapper;
import com.delivery.SuAl.model.enums.PromoStatus;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromoServiceImpl implements PromoService {
    private final PromoRepository promoRepository;
    private final PromoUsageRepository promoUsageRepository;
    private final PromoMapper promoMapper;

    @Override
    @Transactional
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
    @Transactional(readOnly = true)
    public PromoResponse getPromoById(Long id) {
        log.info("Getting promo with id: {}", id);

        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo not found with id: " + id));

        PromoResponse response = promoMapper.toResponse(promo);

        if (promo.getPromoStatus() == PromoStatus.ACTIVE && isExpired(promo)) {
            response.setPromoStatus(PromoStatus.EXPIRED);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromoResponse> getAllPromos(Pageable pageable) {
        log.info("Getting all promos with page: {}", pageable);

        Page<Promo> promos = promoRepository.findAll(pageable);

        return promos.map(promo -> {
            PromoResponse response = promoMapper.toResponse(promo);

            if (promo.getPromoStatus() == PromoStatus.ACTIVE && isExpired(promo)) {
                response.setPromoStatus(PromoStatus.EXPIRED);
            }
            return response;
        });
    }

    @Override
    @Transactional
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

            if (promo.getPromoStatus() == PromoStatus.EXPIRED) {
                LocalDate now = LocalDate.now();
                if (!now.isBefore(newValidFrom) && !now.isAfter(newValidTo)) {
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
    @Transactional
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
        log.info("Validation promo code: {} for customer: {}", request.getPromoCode(), request.getCustomerId());

        try {
            Promo promo = promoRepository.findByPromoCode(request.getPromoCode())
                    .orElseThrow(() -> new NotFoundException("Promo not found with code: " + request.getPromoCode()));

            validatePromoEligibility(promo, request.getCustomerId(), request.getOrderAmount());

            BigDecimal estimatedDiscount = promo.calculateDiscount(request.getOrderAmount());

            Integer customerUsageCount = promoUsageRepository.countUsagesByCustomerAndPromo(
                    request.getCustomerId(), promo.getId());

            Boolean customerCanUse = promo.getMaxUsesPerCustomer() == null || customerUsageCount < promo.getMaxUsesPerCustomer();

            log.info("Promo validation successful. Estimated discount: {}", estimatedDiscount);

            return ValidatePromoResponse.builder()
                    .isValid(true)
                    .message("Promo code is valid")
                    .promoResponse(promoMapper.toResponse(promo))
                    .estimatedDiscount(estimatedDiscount)
                    .customerCanUse(customerCanUse)
                    .customerUsageCount(customerUsageCount)
                    .build();
        } catch (NotFoundException e) {
            log.warn("Promo not found: {}", e.getMessage());
            return buildFailureResponse(e.getMessage());
        } catch (NotValidException | PromoUsageLimitExceededException e) {
            log.warn("Promo validation failed: {}", e.getMessage());
            return buildFailureResponse(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Promo calculation error: {}", e.getMessage());
            return buildFailureResponse(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApplyPromoResponse applyPromo(ApplyPromoRequest request) {
        log.info("Applying promo code: {} for customer: {}", request.getPromoCode(), request.getCustomerId());

        Promo promo = promoRepository.findByPromoCodeWithLock(request.getPromoCode())
                .orElseThrow(() -> new NotFoundException("Promo not found"));

        if (!promo.canBeUsed()) {
            throw new PromoUsageLimitExceededException("Promo usage limit exceeded");
        }

        validatePromoEligibility(promo, request.getCustomerId(), request.getOrderAmount());

        BigDecimal discountAmount;
        try {
            discountAmount = promo.calculateDiscount(request.getOrderAmount());
        } catch (IllegalArgumentException e) {
            throw new NotValidException(e.getMessage());
        }

        PromoUsage promoUsage = new PromoUsage();

        Customer customer = new Customer();
        customer.setId(request.getCustomerId());
        promoUsage.setCustomer(customer);

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

        log.info("Promo applied successfully. Total uses: {}/{}, Discount: {}, Final amount: {}",
                promo.getCurrentTotalUses(),
                promo.getMaxTotalUses(),
                discountAmount,
                finalAmount);

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

    @Override
    @Transactional
    public void releasePromoUsageByOrder(Long orderId) {
        log.info("Releasing promo usages for order ID: {}", orderId);

        List<PromoUsage> usages = promoUsageRepository.findByOrderId(orderId);

        if (usages.isEmpty()) {
            log.debug("No promo usages found for order {}", orderId);
            return;
        }

        log.info("Found {} promo usage(s) to release for order {}", usages.size(), orderId);

        for (PromoUsage usage : usages) {
            releasePromoUsage(usage.getPromo().getId());
        }

        int deletedCount = promoUsageRepository.deleteByOrderId(orderId);
        log.info("Released and deleted {} promo usage records for order {}", deletedCount, orderId);
    }

    @Transactional
    public void releasePromoUsage(Long promoId) {
        log.info("Releasing promo usage for promo ID: {}", promoId);

        promoRepository.findByIdWithLock(promoId)
                .ifPresentOrElse(
                        promo -> {
                            promo.decrementUses();
                            promoRepository.save(promo);
                            log.info("Released promo {} usage. Current uses: {}/{}",
                                    promoId, promo.getCurrentTotalUses(), promo.getMaxTotalUses());
                        },
                        () -> log.warn("Cannot release promo {} - not found", promoId)
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Promo getPromoEntityByCode(String promoCode) {
        return promoRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new NotFoundException("Promo not found with code: " + promoCode));
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireOldPromos() {
        log.info("Running scheduled promo expiration job");

        LocalDate now = LocalDate.now();

        List<Promo> promosToExpire = promoRepository.findAll().stream()
                .filter(promo -> promo.getPromoStatus() == PromoStatus.ACTIVE)
                .filter(promo -> now.isAfter(promo.getValidTo()))
                .toList();

        if (promosToExpire.isEmpty()) {
            log.info("No promos to expire");
            return;
        }

        promosToExpire.forEach(promo -> {
            promo.setPromoStatus(PromoStatus.EXPIRED);
            log.info("Expired promo: {} (valid until {})",
                    promo.getPromoCode(), promo.getValidTo());
        });

        promoRepository.saveAll(promosToExpire);

        log.info("Expired {} promo(s) automatically", promosToExpire.size());
    }

    private boolean isExpired(Promo promo) {
        if (promo.getPromoStatus() != PromoStatus.ACTIVE){
            return true;
        }
        LocalDate now = LocalDate.now();
        return now.isAfter(promo.getValidTo());
    }

    private void validatePromoEligibility(Promo promo, Long customerId, BigDecimal orderAmount) {
        LocalDate now = LocalDate.now();

        if (promo.getPromoStatus() != PromoStatus.ACTIVE) {
            throw new NotValidException("Promo status is not ACTIVE. Current status: " + promo.getPromoStatus());
        }

        if (now.isBefore(promo.getValidFrom())) {
            throw new NotValidException("Promo is not yet valid. Valid from: " + promo.getValidFrom());
        }

        if (now.isAfter(promo.getValidTo())) {
            throw new NotValidException("Promo has expired. Valid until: " + promo.getValidTo());
        }

        if (!promo.canBeUsed()) {
            log.warn("Promo {} has reached total usage limit: {}/{}",
                    promo.getPromoCode(), promo.getCurrentTotalUses(), promo.getMaxTotalUses());
            throw new PromoUsageLimitExceededException("Promo has reached the maximum total limit.");
        }

        if (promo.getMaxUsesPerCustomer() != null) {
            Integer customerUsageCount = promoUsageRepository.countUsagesByCustomerAndPromo(customerId, promo.getId());
            if (customerUsageCount >= promo.getMaxUsesPerCustomer()) {
                throw new PromoUsageLimitExceededException(
                        String.format("You have already used this promo %d times. Maximum allowed: %d",
                                customerUsageCount, promo.getMaxUsesPerCustomer())
                );
            }
        }

        if (orderAmount.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new NotValidException(
                    String.format("Order amount %.2f is below minimum required amount %.2f",
                            orderAmount, promo.getMinOrderAmount())
            );
        }
    }

    private ValidatePromoResponse buildFailureResponse(String message) {
        return ValidatePromoResponse.builder()
                .isValid(false)
                .message(message)
                .promoResponse(null)
                .estimatedDiscount(BigDecimal.ZERO)
                .customerCanUse(false)
                .customerUsageCount(0)
                .build();
    }
}