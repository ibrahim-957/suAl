package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.entity.CampaignUsage;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.CampaignUsageLimitExceededException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.NotValidException;
import com.delivery.SuAl.mapper.CampaignMapper;
import com.delivery.SuAl.model.CampaignStatus;
import com.delivery.SuAl.model.request.marketing.ApplyCampaignRequest;
import com.delivery.SuAl.model.request.marketing.CreateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.UpdateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ValidateCampaignRequest;
import com.delivery.SuAl.model.response.marketing.ApplyCampaignResponse;
import com.delivery.SuAl.model.response.marketing.CampaignResponse;
import com.delivery.SuAl.model.response.marketing.ValidateCampaignResponse;
import com.delivery.SuAl.repository.CampaignRepository;
import com.delivery.SuAl.repository.CampaignUsageRepository;
import com.delivery.SuAl.repository.ProductRepository;
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
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository campaignRepository;
    private final CampaignUsageRepository campaignUsageRepository;
    private final ProductRepository productRepository;
    private final CampaignMapper campaignMapper;


    @Override
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        if (campaignRepository.existsByCampaignId(request.getCampaignId()))
            throw new AlreadyExistsException("Campaign already exists with id: " + request.getCampaignId());

        Product buyProduct = productRepository.findById(request.getBuyProductId())
                .orElseThrow(() -> new NotValidException("Buy product not found with id: " + request.getBuyProductId()));

        Product freeProduct = productRepository.findById(request.getFreeProductId())
                .orElseThrow(() -> new NotValidException("Free product not found with id: " + request.getFreeProductId()));

        Campaign campaign = campaignMapper.toEntity(request);
        campaign.setBuyProduct(buyProduct);
        campaign.setFreeProduct(freeProduct);

        Campaign savedCampaign = campaignRepository.save(campaign);

        log.info("Campaign created with id: {}", savedCampaign.getId());
        return campaignMapper.toResponse(savedCampaign);
    }

    @Override
    public CampaignResponse getCampaignById(Long id) {
        log.info("Get campaign with id: {}", id);
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new NotValidException("Campaign not found with id: " + id));

        checkAndUpdateExpiredStatus(campaign);
        return campaignMapper.toResponse(campaign);
    }

    @Override
    public Page<CampaignResponse> getCampaigns(Pageable pageable) {
        log.info("get All campaigns with pageable: {}", pageable);
        Page<Campaign> campaigns = campaignRepository.findAll(pageable);

        campaigns.getContent().forEach(this::checkAndUpdateExpiredStatus);

        return campaigns.map(campaignMapper::toResponse);
    }

    @Override
    public CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new NotValidException("Campaign not found with id: " + id));

        LocalDate oldValidFrom = campaign.getValidFrom();
        LocalDate oldValidTo = campaign.getValidTo();

        campaignMapper.updateEntityFromRequest(request, campaign);

        if (request.getValidFrom() != null || request.getValidTo() != null) {
            LocalDate newValidFrom = request.getValidFrom() != null ? request.getValidFrom() : oldValidFrom;
            LocalDate newValidTo = request.getValidTo() != null ? request.getValidTo() : oldValidTo;

            if (campaign.getCampaignStatus() == CampaignStatus.EXPIRED) {
                LocalDate now = LocalDate.now();
                if (!now.isBefore(newValidFrom) && !now.isAfter(newValidTo)) {
                    campaign.setCampaignStatus(CampaignStatus.ACTIVE);
                    log.info("Campaign {} reactivated due to valid date range", id);
                }
            }
        }

        Campaign updatedCampaign = campaignRepository.save(campaign);
        log.info("Campaign updated with id: {}", id);

        return campaignMapper.toResponse(updatedCampaign);
    }

    @Override
    public void deleteCampaignById(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new NotValidException("Campaign not found with id: " + id));

        campaign.setCampaignStatus(CampaignStatus.INACTIVE);
        campaignRepository.save(campaign);

        log.info("Campaign soft deleted with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateCampaignResponse validateCampaign(ValidateCampaignRequest request) {
        log.info("Validating campaign: {} for user: {}", request.getCampaignId(), request.getUserId());

        try {
            Campaign campaign = campaignRepository.findByCampaignId(request.getCampaignId())
                    .orElseThrow(() -> new NotValidException("Campaign not found with id: " + request.getCampaignId()));

            validateCampaignEligibility(campaign, request.getUserId(),
                    request.getBuyProductId());

            if (request.getBuyQuantity() < campaign.getBuyQuantity()) {
                throw new NotValidException(
                        String.format("Required buy quantity: %d, provided: %d", request.getBuyQuantity(), campaign.getBuyQuantity())
                );
            }
            int multiplier = request.getBuyQuantity() / campaign.getBuyQuantity();
            int totalFreeQuantity = campaign.getFreeQuantity() * multiplier;

            BigDecimal estimatedBonusValue = campaign.getFreeProduct().getPrices().getLast().getSellPrice()
                    .multiply(BigDecimal.valueOf(totalFreeQuantity));

            Integer userUsageCount = campaignUsageRepository.countUsagesByUserAndCampaign(
                    request.getUserId(), campaign.getId());

            Boolean userCanUse = campaign.getMaxUsesPerUser() == null
                    || userUsageCount < campaign.getMaxUsesPerUser();

            log.info("Campaign validation successful. Free products: {}, Value: {}",
                    totalFreeQuantity, estimatedBonusValue);

            return ValidateCampaignResponse.builder()
                    .isValid(true)
                    .message("Campaign is valid!")
                    .campaignResponse(campaignMapper.toResponse(campaign))
                    .freeQuantity(totalFreeQuantity)
                    .freeProductId(campaign.getFreeProduct().getId())
                    .freeProductName(campaign.getFreeProduct().getName())
                    .estimatedBonusValue(estimatedBonusValue)
                    .userCanUse(userCanUse)
                    .userUsageCount(userUsageCount)
                    .build();
        } catch (NotFoundException e) {
            log.warn("Campaign not found: {}", e.getMessage());
            return ValidateCampaignResponse.builder()
                    .isValid(false)
                    .message(e.getMessage())
                    .campaignResponse(null)
                    .freeQuantity(0)
                    .freeProductId(null)
                    .freeProductName(null)
                    .estimatedBonusValue(BigDecimal.ZERO)
                    .userCanUse(false)
                    .userUsageCount(0)
                    .build();
        } catch (NotValidException | CampaignUsageLimitExceededException e) {
            log.warn("Campaign validation failed: {}", e.getMessage());
            return ValidateCampaignResponse.builder()
                    .isValid(false)
                    .message(e.getMessage())
                    .campaignResponse(null)
                    .freeQuantity(0)
                    .freeProductId(null)
                    .freeProductName(null)
                    .estimatedBonusValue(BigDecimal.ZERO)
                    .userCanUse(false)
                    .userUsageCount(0)
                    .build();
        }
    }

    @Override
    public ApplyCampaignResponse applyCampaign(ApplyCampaignRequest request) {
        Campaign campaign = campaignRepository.findByCampaignId(request.getCampaignId())
                .orElseThrow(() -> new NotFoundException("campaignId" + request.getCampaignId()));

        validateCampaignEligibility(campaign, request.getUserId(), request.getBuyProductId());

        if (request.getBuyQuantity() < campaign.getBuyQuantity()) {
            throw new NotValidException(
                    String.format(
                            "Required buy quantity: %d, provided: %d",
                            campaign.getBuyQuantity(), request.getBuyQuantity()
                    )
            );
        }

        int multiplier = request.getBuyQuantity() / campaign.getBuyQuantity();
        int totalFreeQuantity = campaign.getFreeQuantity() * multiplier;

        CampaignUsage campaignUsage = new CampaignUsage();

        User user = new User();
        user.setId(request.getUserId());
        campaignUsage.setUser(user);

        if (request.getOrderId() != null) {
            Order order = new Order();
            order.setId(request.getOrderId());
            campaignUsage.setOrder(order);
        }

        campaignUsage.setCampaign(campaign);

        CampaignUsage savedUsage = campaignUsageRepository.save(campaignUsage);

        campaign.incrementUses();
        campaignRepository.save(campaign);

        log.info("Campaign applied successfully. Free products: {}", totalFreeQuantity);

        return ApplyCampaignResponse.builder()
                .success(true)
                .message("Campaign applied successfully")
                .campaignUsageId(savedUsage.getId())
                .campaignId(campaign.getCampaignId())
                .campaignName(campaign.getName())
                .freeProductId(campaign.getFreeProduct().getId())
                .freeProductName(campaign.getFreeProduct().getName())
                .freeQuantity(totalFreeQuantity)
                .bonusValue(campaign.getFreeProduct().getPrices()
                        .getLast().getSellPrice().multiply(java.math.BigDecimal.valueOf(totalFreeQuantity)))
                .build();
    }

    private void checkAndUpdateExpiredStatus(Campaign campaign) {
        if (campaign.getCampaignStatus() == CampaignStatus.ACTIVE) {
            LocalDate now = LocalDate.now();
            if (now.isAfter(campaign.getValidTo())) {
                campaign.setCampaignStatus(CampaignStatus.EXPIRED);
                campaignRepository.save(campaign);
                log.info("Campaign {} status change to EXPIRED", campaign.getId());
            }
        }
    }

    private void validateCampaignEligibility(Campaign campaign, Long userId,
                                             Long buyProductId) {
        LocalDate now = LocalDate.now();

        if (campaign.getCampaignStatus() != CampaignStatus.ACTIVE) {
            throw new NotValidException("Campaign is not active. Current status: " + campaign.getCampaignStatus());
        }

        if (now.isBefore(campaign.getValidFrom())) {
            throw new NotValidException("Campaign is not yet valid. Valid from: " + campaign.getValidFrom());
        }

        if (now.isAfter(campaign.getValidTo())) {
            throw new NotValidException("Campaign has expired. Valid until: " + campaign.getValidTo());
        }

        if (campaign.hasReachedTotalLimit()) {
            throw new CampaignUsageLimitExceededException("Campaign has reached maximum total usage limit");
        }

        if (campaign.getMaxUsesPerUser() != null) {
            Integer userUsageCount = campaignUsageRepository.countUsagesByUserAndCampaign(userId, campaign.getId());
            if (userUsageCount >= campaign.getMaxUsesPerUser()) {
                throw new CampaignUsageLimitExceededException(
                        String.format("You have already used this campaign %d times. Maximum allowed: %d",
                                userUsageCount, campaign.getMaxUsesPerUser())
                );
            }
        }

        if (!campaign.getBuyProduct().getId().equals(buyProductId)) {
            throw new NotValidException(
                    String.format("Campaign requires product ID: %d, but provided: %d",
                            campaign.getBuyProduct().getId(), buyProductId)
            );
        }
    }
}