package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.entity.CampaignUsage;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.NotValidException;
import com.delivery.SuAl.helper.EligibleCampaignInfo;
import com.delivery.SuAl.helper.FreeProductSummary;
import com.delivery.SuAl.mapper.CampaignMapper;
import com.delivery.SuAl.model.enums.CampaignStatus;
import com.delivery.SuAl.model.request.marketing.ApplyCampaignRequest;
import com.delivery.SuAl.model.request.marketing.CreateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.GetEligibleCampaignsRequest;
import com.delivery.SuAl.model.request.marketing.UpdateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ValidateCampaignRequest;
import com.delivery.SuAl.model.response.marketing.ApplyCampaignResponse;
import com.delivery.SuAl.model.response.marketing.CampaignResponse;
import com.delivery.SuAl.model.response.marketing.EligibleCampaignsResponse;
import com.delivery.SuAl.model.response.marketing.ValidateCampaignResponse;
import com.delivery.SuAl.repository.CampaignRepository;
import com.delivery.SuAl.repository.CampaignUsageRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository campaignRepository;
    private final CampaignUsageRepository campaignUsageRepository;
    private final ProductRepository productRepository;
    private final CampaignMapper campaignMapper;
    @Lazy
    private final OrderService orderService;
    private final UserService userService;

    public CampaignServiceImpl(
            CampaignRepository campaignRepository,
            CampaignUsageRepository campaignUsageRepository,
            ProductRepository productRepository,
            CampaignMapper campaignMapper,
            @Lazy OrderService orderService,
            UserService userService
    ) {
        this.campaignRepository = campaignRepository;
        this.campaignUsageRepository = campaignUsageRepository;
        this.productRepository = productRepository;
        this.campaignMapper = campaignMapper;
        this.orderService = orderService;
        this.userService = userService;
    }


    @Override
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        log.info("Creating campaign with campaignCode: {}", request.getCampaignCode());

        if (campaignRepository.existsByCampaignCode(request.getCampaignCode()))
            throw new AlreadyExistsException("Campaign already exists with id: " + request.getCampaignCode());

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
                .orElseThrow(() -> new NotFoundException("Campaign not found with id: " + id));

//        checkAndUpdateExpiredStatus(campaign);
        return campaignMapper.toResponse(campaign);
    }

    @Override
    public Page<CampaignResponse> getCampaigns(Pageable pageable) {
        log.info("get All campaigns with pageable: {}", pageable);
        Page<Campaign> campaigns = campaignRepository.findAll(pageable);

//        campaigns.getContent().forEach(this::checkAndUpdateExpiredStatus);

        return campaigns.map(campaignMapper::toResponse);
    }

    @Override
    public CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request) {
        log.info("Update campaign with id: {}", id);
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
    public ValidateCampaignResponse validateCampaign(ValidateCampaignRequest request) {
        log.info("Validating campaign: {} for user: {}", request.getCampaignCode(), request.getUserId());

        Campaign campaign = campaignRepository.findByCampaignCode(request.getCampaignCode())
                .orElseThrow(() -> new NotFoundException("Campaign not found with id: " + request.getCampaignCode()));

        User user = userService.getUserEntityById(request.getUserId());

        ValidateCampaignResponse.ValidateCampaignResponseBuilder responseBuilder = ValidateCampaignResponse.builder();

        boolean isValid = true;
        StringBuilder messageBuilder = new StringBuilder();

        boolean meetsDateRequirement = campaign.isActive();
        responseBuilder.meetsDateRequirement(meetsDateRequirement);
        if (!meetsDateRequirement) {
            isValid = false;
            messageBuilder.append("Campaign is not active or has expired");
        }

        boolean meetsTotalUsageLimit = !campaign.hasReachedTotalLimit();
        if (!meetsTotalUsageLimit) {
            isValid = false;
            messageBuilder.append("Campaign has reached the total usage limit");
        }

        Integer basketQuantity = request.getProductQuantities().getOrDefault(campaign.getBuyProduct().getId(), 0);
        boolean meetsQuantityRequirement = basketQuantity >= campaign.getBuyQuantity();
        responseBuilder.meetsQuantityRequirement(meetsQuantityRequirement);
        if (!meetsQuantityRequirement) {
            isValid = false;
            messageBuilder.append(String.format("Required quantity: %d, but basket has: %d. ",
                    campaign.getBuyQuantity(), basketQuantity));
        }

        boolean meetsFirstOrderRequirement = true;
        if (campaign.isFirstOrderOnly()) {
            meetsFirstOrderRequirement = isFirstOrder(user.getId());
            responseBuilder.meetsFirstOrderRequirement(meetsFirstOrderRequirement);
            if (!meetsFirstOrderRequirement) {
                isValid = false;
                messageBuilder.append("This campaign is only for first orders. ");
            }
        } else {
            responseBuilder.meetsFirstOrderRequirement(true);
        }

        int userUsageCount = getUserCampaignUsageCount(user.getId(), campaign.getCampaignCode());
        boolean meetsUsageLimitRequirement = true;
        Integer usageRemaining = null;
        if (campaign.getMaxUsesPerUser() != null) {
            meetsUsageLimitRequirement = userUsageCount < campaign.getMaxUsesPerUser();
            responseBuilder.meetsUsageLimitRequirement(meetsUsageLimitRequirement);
            responseBuilder.userUsageCount(userUsageCount);
            responseBuilder.usageRemaining(Math.min(0, usageRemaining));

            if (!meetsUsageLimitRequirement) {
                isValid = false;
                messageBuilder.append("User has reached the maximum usage limit for this campaign. ");
            }
        } else {
            responseBuilder.meetsUsageLimitRequirement(true);
            responseBuilder.userUsageCount(userUsageCount);
        }

        responseBuilder.meetsPromoAbsenceRequirement(true);

        Integer freeQuantity = null;
        BigDecimal estimatedBonusValue = null;

        if (isValid && meetsQuantityRequirement) {
            freeQuantity = calculateEligibleFreeQuantity(basketQuantity, campaign.getBuyQuantity(), campaign.getFreeQuantity());
            estimatedBonusValue = calculateBonusValue(campaign).multiply(BigDecimal.valueOf(freeQuantity / campaign.getMaxUsesPerUser()));
        }

        String finalMessage = isValid ? "Campaign is valid and can be applied" : messageBuilder.toString().trim();

        return responseBuilder
                .isValid(isValid)
                .message(finalMessage)
                .campaignResponse(campaignMapper.toResponse(campaign))
                .freeQuantity(freeQuantity)
                .freeProductId(campaign.getBuyProduct().getId())
                .freeProductName(campaign.getBuyProduct().getName())
                .estimatedBonusValue(estimatedBonusValue)
                .userCanUse(isValid)
                .build();
    }

    @Override
    public ApplyCampaignResponse applyCampaign(ApplyCampaignRequest request) {
        log.info("Applying campaign: {} for user: {} on order: {}",
                request.getCampaignCode(), request.getUserId(), request.getOrder());

        Order order = request.getOrder();

        Campaign campaign = campaignRepository.findByCampaignCode(request.getCampaignCode())
                .orElseThrow(() -> new NotFoundException("Campaign not found with code: " + request.getCampaignCode()));

        User user = userService.getUserEntityById(request.getUserId());

        if (!campaign.isActive()) {
            return ApplyCampaignResponse.builder()
                    .success(false)
                    .message("Campaign is not active or has expired")
                    .build();
        }

        if (campaign.hasReachedTotalLimit()) {
            return ApplyCampaignResponse.builder()
                    .success(false)
                    .message("Campaign has reached the total limit")
                    .build();
        }

        if (campaign.getMaxUsesPerUser() != null) {
            int userUsageCount = getUserCampaignUsageCount(user.getId(), campaign.getCampaignCode());
            if (userUsageCount >= campaign.getMaxUsesPerUser()) {
                return ApplyCampaignResponse.builder()
                        .success(false)
                        .message("Campaign has reached the maximum usage limit")
                        .build();
            }
        }

        int buyQuantity = getProductQuantityFromOrder(order, campaign.getBuyProduct().getId());

        if (buyQuantity < campaign.getBuyQuantity()) {
            return ApplyCampaignResponse.builder()
                    .success(false)
                    .message("Order does not meet quantity requirement")
                    .build();
        }

        int freeQuantity = calculateEligibleFreeQuantity(buyQuantity, campaign.getBuyQuantity(), campaign.getFreeQuantity());

        BigDecimal bonusValue = calculateBonusValue(campaign)
                .multiply(BigDecimal.valueOf(freeQuantity / campaign.getFreeQuantity()));

        CampaignUsage campaignUsage = new CampaignUsage();
        campaignUsage.setUser(user);
        campaignUsage.setCampaign(campaign);
        campaignUsage.setOrder(order);
        campaignUsage.setBuyProduct(campaign.getBuyProduct());
        campaignUsage.setBuyQuantity(buyQuantity);
        campaignUsage.setFreeProduct(campaign.getFreeProduct());
        campaignUsage.setFreeQuantity(freeQuantity);
        campaignUsage.setBonusValue(bonusValue);

        campaignUsageRepository.save(campaignUsage);

        campaign.incrementUses();
        campaignRepository.save(campaign);

        log.info("Campaign applied successfully: {} - Free quantity: {}, Bonus value: {}",
                campaign.getCampaignCode(), freeQuantity, bonusValue);
        return ApplyCampaignResponse.builder()
                .success(true)
                .message("Campaign applied successfully")
                .campaignUsageId(campaignUsage.getId())
                .campaignName(campaign.getName())
                .campaignCode(campaign.getCampaignCode())
                .freeProductId(campaign.getFreeProduct().getId())
                .freeProductName(campaign.getFreeProduct().getName())
                .freeQuantity(freeQuantity)
                .bonusValue(bonusValue)
                .build();
    }

    @Override
    public EligibleCampaignsResponse getEligibleCampaigns(GetEligibleCampaignsRequest request) {
        log.info("Getting eligible campaigns for user: {}", request.getUserId());

        User user = userService.getUserEntityById(request.getUserId());

        List<Campaign> activeCampaigns = campaignRepository.findByCampaignStatus(CampaignStatus.ACTIVE);

        List<EligibleCampaignInfo> eligibleCampaignInfos = new ArrayList<>();
        BigDecimal totalCampaignDiscount = BigDecimal.ZERO;
        Map<Long, FreeProductSummary> freeProductsMap = new HashMap<>();

        for (Campaign campaign : activeCampaigns) {
            if (!campaign.isActive()) {
                continue;
            }

            if (campaign.hasReachedTotalLimit()) {
                continue;
            }

            if (Boolean.TRUE.equals(request.getWillUsePromoCode()) && Boolean.TRUE.equals(campaign.getRequiresPromoAbsence())) {
                EligibleCampaignInfo info = buildNotAppliedCampaignInfo(campaign, request, "Cannot be used with promo codes");
                eligibleCampaignInfos.add(info);
                continue;
            }

            Long buyProductId = campaign.getBuyProduct().getId();
            Integer basketQuantity = request.getProductQuantities().getOrDefault(buyProductId, 0);

            if (basketQuantity == 0) {
                continue;
            }

            EligibleCampaignInfo.EligibleCampaignInfoBuilder infoBuilder = EligibleCampaignInfo.builder()
                    .campaignCode(campaign.getCampaignCode())
                    .campaignName(campaign.getName())
                    .description(campaign.getDescription())
                    .campaignType(campaign.getCampaignType())
                    .buyProductId(buyProductId)
                    .buyProductName(campaign.getBuyProduct().getName())
                    .buyQuantityRequired(campaign.getBuyQuantity())
                    .buyQuantityInBasket(basketQuantity)
                    .freeProductId(campaign.getFreeProduct().getId())
                    .freeProductName(campaign.getFreeProduct().getName())
                    .freeQuantity(campaign.getFreeQuantity());

            if (basketQuantity < campaign.getBuyQuantity()) {
                infoBuilder.willBeApplied(false)
                        .notAppliedReason(String.format("Need %d more items to qualify",
                                campaign.getBuyQuantity() - basketQuantity));
                eligibleCampaignInfos.add(infoBuilder.build());
                continue;
            }

            if (campaign.isFirstOrderOnly()) {
                boolean isFirstOrder = isFirstOrder(user.getId());
                if (!isFirstOrder) {
                    infoBuilder.willBeApplied(false)
                            .notAppliedReason("Campaign is only for first orders");
                    eligibleCampaignInfos.add(infoBuilder.build());
                    continue;
                }
            }

            if (campaign.getMinDaysSinceRegistration() != null && campaign.getMinDaysSinceRegistration() > 0) {
                int daysSinceRegistration = getDaysSinceRegistration(user);
                if (daysSinceRegistration < campaign.getMinDaysSinceRegistration()) {
                    infoBuilder.willBeApplied(false)
                            .notAppliedReason(String.format("Must be registered for %d more days",
                                    campaign.getMinDaysSinceRegistration() - daysSinceRegistration));
                    eligibleCampaignInfos.add(infoBuilder.build());
                    continue;
                }
            }

            if (campaign.getMaxUsesPerUser() != null) {
                int userUsageCount = getUserCampaignUsageCount(user.getId(), campaign.getCampaignCode());
                if (userUsageCount >= campaign.getMaxUsesPerUser()) {
                    infoBuilder.willBeApplied(false)
                            .notAppliedReason("You have reached the maximum usage limit for this campaign");
                    eligibleCampaignInfos.add(infoBuilder.build());
                    continue;
                }
            }

            int eligibleFreeQuantity = calculateEligibleFreeQuantity(
                    basketQuantity,
                    campaign.getBuyQuantity(),
                    campaign.getFreeQuantity()
            );

            BigDecimal freeProductPrice = getFreeProductPrice(campaign.getFreeProduct());
            BigDecimal bonusValue = freeProductPrice.multiply(BigDecimal.valueOf(eligibleFreeQuantity));

            boolean hasDeposit = Boolean.TRUE.equals(campaign.getFreeProduct().getHasDeposit());
            BigDecimal depositPerUnit = hasDeposit ? campaign.getFreeProduct().getDepositAmount() : BigDecimal.ZERO;
            BigDecimal totalDeposit = depositPerUnit.multiply(BigDecimal.valueOf(eligibleFreeQuantity));

            EligibleCampaignInfo info = infoBuilder
                    .freeQuantity(eligibleFreeQuantity)
                    .freeProductPrice(freeProductPrice)
                    .bonusValue(bonusValue)
                    .freeProductHasDeposit(hasDeposit)
                    .depositPerUnit(depositPerUnit)
                    .totalDepositForFree(totalDeposit)
                    .willBeApplied(true)
                    .notAppliedReason(null)
                    .build();

            eligibleCampaignInfos.add(info);

            totalCampaignDiscount = totalCampaignDiscount.add(bonusValue);

            Long freeProductId = campaign.getFreeProduct().getId();
            if (freeProductsMap.containsKey(freeProductId)) {
                FreeProductSummary existing = freeProductsMap.get(freeProductId);
                existing.setTotalQuantity(existing.getTotalQuantity() + eligibleFreeQuantity);
                existing.setTotalValue(existing.getTotalValue().add(bonusValue));
                if (hasDeposit) {
                    existing.setTotalDeposit(existing.getTotalDeposit().add(totalDeposit));
                }
            } else {
                FreeProductSummary summary = FreeProductSummary.builder()
                        .productId(freeProductId)
                        .productName(campaign.getFreeProduct().getName())
                        .totalQuantity(eligibleFreeQuantity)
                        .pricePerUnit(freeProductPrice)
                        .totalValue(bonusValue)
                        .hasDeposit(hasDeposit)
                        .depositPerUnit(depositPerUnit)
                        .totalDeposit(totalDeposit)
                        .build();
                freeProductsMap.put(freeProductId, summary);
            }
        }
        return EligibleCampaignsResponse.builder()
                .eligibleCampaigns(eligibleCampaignInfos)
                .totalCampaignDiscount(totalCampaignDiscount)
                .allFreeProducts(new ArrayList<>(freeProductsMap.values()))
                .build();
    }

    private int getUserCampaignUsageCount(Long userId, String campaignCode) {
        return campaignUsageRepository.countByUserIdAndCampaignCampaignCode(userId, campaignCode);
    }

    private boolean isFirstOrder(Long userId) {
        return orderService.getCompletedOrderCount(userId) == 0;
    }

    private int getDaysSinceRegistration(User user) {
        LocalDateTime registrationDate = user.getCreatedAt();
        return (int) ChronoUnit.DAYS.between(registrationDate, LocalDateTime.now());
    }

    private BigDecimal calculateBonusValue(Campaign campaign) {
        if (campaign.getFreeProduct() == null
                || campaign.getFreeProduct().getPrices() == null
                || campaign.getFreeProduct().getPrices().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return campaign.getFreeProduct().getPrices().getLast().getSellPrice()
                .multiply(BigDecimal.valueOf(campaign.getFreeQuantity()));
    }

    private BigDecimal getFreeProductPrice(Product product) {
        if (product == null || product.getPrices() == null || product.getPrices().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return product.getPrices().getLast().getSellPrice();
    }

    private int calculateEligibleFreeQuantity(int basketQuantity, int buyQuantity, int freeQuantity) {
        return (basketQuantity / buyQuantity) * freeQuantity;
    }

    private int getProductQuantityFromOrder(Order order, Long productId) {
        return order.getOrderDetails().stream()
                .filter(detail -> detail.getProduct().getId().equals(productId))
                .mapToInt(OrderDetail::getCount)
                .sum();
    }

    private EligibleCampaignInfo buildNotAppliedCampaignInfo(Campaign campaign, GetEligibleCampaignsRequest request, String s) {
        Integer basketQuantity = request.getProductQuantities().getOrDefault(campaign.getBuyProduct().getId(), 0);

        return EligibleCampaignInfo.builder()
                .campaignCode(campaign.getCampaignCode())
                .campaignName(campaign.getName())
                .description(campaign.getDescription())
                .campaignType(campaign.getCampaignType())
                .buyProductId(campaign.getBuyProduct().getId())
                .buyProductName(campaign.getBuyProduct().getName())
                .buyQuantityRequired(campaign.getBuyQuantity())
                .buyQuantityInBasket(basketQuantity)
                .freeProductId(campaign.getFreeProduct().getId())
                .freeProductName(campaign.getFreeProduct().getName())
                .freeQuantity(campaign.getFreeQuantity())
                .willBeApplied(false)
                .build();
    }
}