package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Campaign;
import com.delivery.SuAl.entity.CampaignUsage;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.OrderDetail;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.helper.CampaignValidationContext;
import com.delivery.SuAl.helper.EligibleCampaignInfo;
import com.delivery.SuAl.helper.EligibleCampaignResult;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository campaignRepository;
    private final CampaignUsageRepository campaignUsageRepository;
    private final ProductRepository productRepository;
    private final CampaignMapper campaignMapper;
    private final UserService userService;
    private final OrderQueryService orderQueryService;

    @Override
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        log.info("Creating campaign with campaignCode: {}", request.getCampaignCode());

        validateCampaignDoesNotExist(request.getCampaignCode());

        Product buyProduct = findProductById(request.getBuyProductId(), "Buy");
        Product freeProduct = findProductById(request.getFreeProductId(), "Free");

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
        Campaign campaign = findByCampaignById(id);
        return campaignMapper.toResponse(campaign);
    }

    @Override
    public Page<CampaignResponse> getCampaigns(Pageable pageable) {
        log.info("get All campaigns with pageable: {}", pageable);
        Page<Campaign> campaigns = campaignRepository.findAll(pageable);
        return campaigns.map(campaignMapper::toResponse);
    }

    @Override
    public CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request) {
        log.info("Update campaign with id: {}", id);
        Campaign campaign = findByCampaignById(id);

        LocalDate oldValidFrom = campaign.getValidFrom();
        LocalDate oldValidTo = campaign.getValidTo();

        campaignMapper.updateEntityFromRequest(request, campaign);

        reactivateCampaignIfNeeded(campaign, request, oldValidFrom, oldValidTo);

        Campaign updatedCampaign = campaignRepository.save(campaign);
        log.info("Campaign updated with id: {}", id);

        return campaignMapper.toResponse(updatedCampaign);
    }

    @Override
    public void deleteCampaignById(Long id) {
        Campaign campaign = findByCampaignById(id);
        campaign.setCampaignStatus(CampaignStatus.INACTIVE);
        campaignRepository.save(campaign);

        log.info("Campaign soft deleted with id: {}", id);
    }

    @Override
    public ValidateCampaignResponse validateCampaign(ValidateCampaignRequest request) {
        log.info("Validating campaign: {} for user: {}", request.getCampaignCode(), request.getUserId());

        Campaign campaign = findCampaignByCode(request.getCampaignCode());
        User user = userService.getUserEntityById(request.getUserId());

        CampaignValidationContext context = new CampaignValidationContext();

        validateCampaignRules(campaign, user, request, context);
        return buildValidateCampaignResponse(campaign, context);
    }

    @Override
    public ApplyCampaignResponse applyCampaign(ApplyCampaignRequest request) {
        log.info("Applying campaign: {} for user: {} on order: {}",
                request.getCampaignCode(), request.getUserId(), request.getOrder());

        Campaign campaign = findCampaignByCode(request.getCampaignCode());
        User user = userService.getUserEntityById(request.getUserId());

        ApplyCampaignResponse validationFailure = validateCampaignForApplication(campaign, user);

        if (validationFailure != null) {
            return validationFailure;
        }

        int buyQuantity = getProductQuantityFromOrder(request.getOrder(), campaign.getBuyProduct().getId());
        if (buyQuantity < campaign.getBuyQuantity()) {
            return buildFailureResponse("Order does not meet quantity requirements");
        }

        return processCampaignApplication(campaign, user, request.getOrder(), buyQuantity);
    }

    @Override
    public EligibleCampaignsResponse getEligibleCampaigns(GetEligibleCampaignsRequest request) {
        log.info("Getting eligible campaigns for user: {}", request.getUserId());

        User user = userService.getUserEntityById(request.getUserId());
        List<Campaign> activeCampaigns = campaignRepository.findByCampaignStatus(CampaignStatus.ACTIVE);

        List<EligibleCampaignInfo> eligibleCampaignInfos = new ArrayList<>();
        BigDecimal totalCampaignDiscount = BigDecimal.ZERO;
        Map<Long, FreeProductSummary> freeProductMap = new HashMap<>();

        for (Campaign campaign : activeCampaigns) {
            EligibleCampaignResult result = evaluateCampaignEligibility(campaign, user, request);

            if (result.shouldSkip()) {
                continue;
            }

            eligibleCampaignInfos.add(result.getCampaignInfo());

            if (Boolean.TRUE.equals(result.getCampaignInfo().getWillBeApplied())) {
                totalCampaignDiscount = totalCampaignDiscount.add(result.getCampaignInfo().getBonusValue());
                updateFreeProductSummary(freeProductMap, campaign, result.getCampaignInfo());
            }
        }

        return EligibleCampaignsResponse.builder()
                .eligibleCampaigns(eligibleCampaignInfos)
                .totalCampaignDiscount(totalCampaignDiscount)
                .allFreeProducts(new ArrayList<>(freeProductMap.values()))
                .build();
    }

    private void validateCampaignDoesNotExist(String campaignCode) {
        if (campaignRepository.existsByCampaignCode(campaignCode)) {
            throw new AlreadyExistsException("Campaign already exists with id: " + campaignCode);
        }
    }

    private void validateCampaignRules(Campaign campaign, User user, ValidateCampaignRequest request,
                                       CampaignValidationContext context) {
        validateDateRequirement(campaign, context);
        validateTotalUsageLimit(campaign, context);
        validateQuantityRequirement(campaign, request, context);
        validateFirstOrderRequirement(campaign, user, context);
        validateUsageLimitRequirement(campaign, user, context);

        context.setMeetsPromoAbsenceRequirement(true);

        if (context.isValid() && context.isMeetsQuantityRequirement()) {
            calculateEstimatedBonus(campaign, context);
        }
    }

    private void validateDateRequirement(Campaign campaign, CampaignValidationContext context) {
        boolean meetsDateRequirement = campaign.isActive();
        context.setMeetsDateRequirement(meetsDateRequirement);

        if (!meetsDateRequirement) {
            context.markInvalid("Campaign is not active or has expired");
        }
    }

    private void validateTotalUsageLimit(Campaign campaign, CampaignValidationContext context) {
        if (campaign.hasReachedTotalLimit()) {
            context.markInvalid("Campaign has reached the total usage limit");
        }
    }

    private void validateQuantityRequirement(Campaign campaign, ValidateCampaignRequest request,
                                             CampaignValidationContext context) {
        Integer basketQuantity = request.getProductQuantities()
                .getOrDefault(campaign.getBuyProduct().getId(), 0);

        boolean meetsQuantityRequirement = basketQuantity >= campaign.getBuyQuantity();
        context.setMeetsQuantityRequirement(meetsQuantityRequirement);
        context.setBasketQuantity(basketQuantity);

        if (!meetsQuantityRequirement) {
            context.markInvalid(String.format("Required quantity: %d, but basket has: %d. ",
                    campaign.getBuyQuantity(), basketQuantity));
        }
    }

    private void validateFirstOrderRequirement(Campaign campaign, User user, CampaignValidationContext context) {
        if (campaign.isFirstOrderOnly()) {
            boolean meetsFirstOrderRequirement = isFirstOrder(user.getId());
            context.setMeetsFirstOrderRequirement(meetsFirstOrderRequirement);

            if (!meetsFirstOrderRequirement) {
                context.markInvalid("This campaign is only for first orders. ");
            }
        } else {
            context.setMeetsFirstOrderRequirement(true);
        }
    }

    private void validateUsageLimitRequirement(Campaign campaign, User user, CampaignValidationContext context) {
        int userUsageCount = getUserCampaignUsageCount(user.getId(), campaign.getCampaignCode());

        if (campaign.getMaxUsesPerUser() != null) {
            boolean meetsUsageLimitRequirement = userUsageCount < campaign.getMaxUsesPerUser();
            context.setMeetsUsageLimitRequirement(meetsUsageLimitRequirement);
            context.setUserUsageCount(userUsageCount);
            context.setUsageRemaining(campaign.getMaxUsesPerUser() - userUsageCount);

            if (!meetsUsageLimitRequirement) {
                context.markInvalid("User has reached the maximum usage limit for this campaign. ");
            }
        } else {
            context.setMeetsUsageLimitRequirement(true);
            context.setUserUsageCount(userUsageCount);
        }
    }

    private void calculateEstimatedBonus(Campaign campaign, CampaignValidationContext context) {
        int freeQuantity = calculateEligibleFreeQuantity(
                context.getBasketQuantity(),
                campaign.getBuyQuantity(),
                campaign.getFreeQuantity()
        );

        BigDecimal estimatedBonusValue = calculateBonusValue(campaign)
                .multiply(BigDecimal.valueOf(freeQuantity / campaign.getFreeQuantity()));

        context.setFreeQuantity(freeQuantity);
        context.setEstimatedBonusValue(estimatedBonusValue);
    }

    private ApplyCampaignResponse validateCampaignForApplication(Campaign campaign, User user) {
        if (!campaign.isActive()) {
            return buildFailureResponse("Campaign is not active or has expired");
        }

        if (campaign.hasReachedTotalLimit()) {
            return buildFailureResponse("Campaign has reached the total limit");
        }

        if (campaign.getMaxUsesPerUser() != null) {
            int userUsageCount = getUserCampaignUsageCount(user.getId(), campaign.getCampaignCode());
            if (userUsageCount >= campaign.getMaxUsesPerUser()) {
                return buildFailureResponse("Campaign has reached the maximum usage limit");
            }
        }

        return null;
    }

    private ApplyCampaignResponse processCampaignApplication(
            Campaign campaign, User user, Order order, int buyQuantity) {
        int freeQuantity = calculateEligibleFreeQuantity(
                buyQuantity, campaign.getBuyQuantity(), campaign.getFreeQuantity());

        BigDecimal bonusValue = calculateBonusValue(campaign)
                .multiply(BigDecimal.valueOf(freeQuantity / campaign.getFreeQuantity()));

        CampaignUsage campaignUsage = createCampaignUsage(campaign, user, order, buyQuantity, freeQuantity, bonusValue);
        campaignUsageRepository.save(campaignUsage);

        campaign.incrementUses();
        campaignRepository.save(campaign);

        log.info("Campaign applied successfully: {} - Free quantity: {}, Bonus value: {}",
                campaign.getCampaignCode(), freeQuantity, bonusValue);

        return buildSuccessResponse(campaign, campaignUsage, freeQuantity, bonusValue);
    }

    private CampaignUsage createCampaignUsage(
            Campaign campaign, User user, Order order, int buyQuantity,
            int freeQuantity, BigDecimal bonusValue) {
        CampaignUsage campaignUsage = new CampaignUsage();
        campaignUsage.setUser(user);
        campaignUsage.setOrder(order);
        campaignUsage.setCampaign(campaign);
        campaignUsage.setBuyProduct(campaign.getBuyProduct());
        campaignUsage.setBuyQuantity(buyQuantity);
        campaignUsage.setFreeProduct(campaign.getFreeProduct());
        campaignUsage.setFreeQuantity(freeQuantity);
        campaignUsage.setBonusValue(bonusValue);
        return campaignUsage;
    }

    private EligibleCampaignResult evaluateCampaignEligibility(
            Campaign campaign, User user, GetEligibleCampaignsRequest request) {
        if (!campaign.isActive() || campaign.hasReachedTotalLimit()) {
            return EligibleCampaignResult.skip();
        }

        if (Boolean.TRUE.equals(request.getWillUsePromoCode()) &&
                Boolean.TRUE.equals(campaign.getRequiresPromoAbsence())) {
            EligibleCampaignInfo info = buildNotAppliedCampaignInfo(
                    campaign, request, "Cannot be used with promo codes");
            return EligibleCampaignResult.withInfo(info);
        }

        Long buyProductId = campaign.getBuyProduct().getId();
        Integer basketQuantity = request.getProductQuantities().getOrDefault(buyProductId, 0);

        if (basketQuantity == 0) {
            return EligibleCampaignResult.skip();
        }

        return evaluateCampaignRequirements(campaign, user, request, basketQuantity);
    }

    private EligibleCampaignResult evaluateCampaignRequirements(
            Campaign campaign, User user,
            GetEligibleCampaignsRequest request, Integer basketQuantity) {
        EligibleCampaignInfo.EligibleCampaignInfoBuilder infoBuilder = buildBasicCampaignInfo(campaign, basketQuantity);

        if (basketQuantity < campaign.getBuyQuantity()) {
            return EligibleCampaignResult.withInfo(
                    infoBuilder
                            .willBeApplied(false)
                            .notAppliedReason(String.format("Need %d more items to qualify",
                                    campaign.getBuyQuantity() - basketQuantity))
                            .build()
            );
        }

        if (campaign.isFirstOrderOnly() && !isFirstOrder(user.getId())) {
            return EligibleCampaignResult.withInfo(
                    infoBuilder
                            .willBeApplied(false)
                            .notAppliedReason("Campaign is only for first Order")
                            .build()
            );
        }

        if (!meetsUsageLimitRequirement(campaign, user, infoBuilder)) {
            return EligibleCampaignResult.withInfo(infoBuilder.build());
        }

        if (!meetsRegistrationDatsRequirement(campaign, user, infoBuilder)) {
            return EligibleCampaignResult.withInfo(infoBuilder.build());
        }

        return calculateCampaignBenefits(campaign, basketQuantity, infoBuilder);
    }

    private boolean meetsRegistrationDatsRequirement(
            Campaign campaign,
            User user,
            EligibleCampaignInfo.EligibleCampaignInfoBuilder infoBuilder) {
        if (campaign.getMinDaysSinceRegistration() != null && campaign.getMinDaysSinceRegistration() > 0) {
            int daysSinceRegistration = getDaysSinceRegistration(user);
            if (daysSinceRegistration < campaign.getMinDaysSinceRegistration()) {
                infoBuilder.willBeApplied(false)
                        .notAppliedReason(String.format("Must be registered for %d more days",
                                campaign.getMinDaysSinceRegistration() - daysSinceRegistration));
                return false;
            }
        }
        return true;
    }

    private boolean meetsUsageLimitRequirement(
            Campaign campaign,
            User user,
            EligibleCampaignInfo.EligibleCampaignInfoBuilder infoBuilder) {
        if (campaign.getMaxUsesPerUser() != null) {
            int userUsageCount = getUserCampaignUsageCount(user.getId(), campaign.getCampaignCode());
            if (userUsageCount >= campaign.getMaxUsesPerUser()) {
                infoBuilder.willBeApplied(false)
                        .notAppliedReason("You have reached the maximum usage limit for this campaign");
                return false;
            }
        }
        return true;
    }

    private EligibleCampaignResult calculateCampaignBenefits(
            Campaign campaign,
            Integer basketQuantity,
            EligibleCampaignInfo.EligibleCampaignInfoBuilder infoBuilder) {
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

        return EligibleCampaignResult.withInfo(info);
    }

    private void updateFreeProductSummary(
            Map<Long, FreeProductSummary> freeProductsMap,
            Campaign campaign,
            EligibleCampaignInfo campaignInfo) {
        Long freeProductId = campaign.getFreeProduct().getId();

        if (freeProductsMap.containsKey(freeProductId)) {
            updateExistingFreeProductSummary(freeProductsMap.get(freeProductId), campaignInfo);
        } else {
            freeProductsMap.put(freeProductId, createNewFreeProductSummary(campaign, campaignInfo));
        }
    }

    private void updateExistingFreeProductSummary(FreeProductSummary existing, EligibleCampaignInfo campaignInfo) {
        existing.setTotalQuantity(existing.getTotalQuantity() + campaignInfo.getFreeQuantity());
        existing.setTotalValue(existing.getTotalValue().add(campaignInfo.getBonusValue()));
    }

    private FreeProductSummary createNewFreeProductSummary(Campaign campaign, EligibleCampaignInfo campaignInfo) {
        return FreeProductSummary.builder()
                .productId(campaign.getFreeProduct().getId())
                .productName(campaign.getFreeProduct().getName())
                .totalQuantity(campaignInfo.getFreeQuantity())
                .pricePerUnit(campaignInfo.getFreeProductPrice())
                .totalValue(campaignInfo.getBonusValue())
                .hasDeposit(campaignInfo.getFreeProductHasDeposit())
                .depositPerUnit(campaignInfo.getDepositPerUnit())
                .totalDeposit(campaignInfo.getTotalDepositForFree())
                .build();
    }

    private EligibleCampaignInfo.EligibleCampaignInfoBuilder buildBasicCampaignInfo(Campaign campaign,
                                                                                    Integer basketQuantity) {
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
                .freeQuantity(campaign.getFreeQuantity());
    }

    private EligibleCampaignInfo buildNotAppliedCampaignInfo(
            Campaign campaign, GetEligibleCampaignsRequest request, String reason
    ) {
        Integer basketQuantity = request.getProductQuantities()
                .getOrDefault(campaign.getBuyProduct().getId(), 0);

        return buildBasicCampaignInfo(campaign, basketQuantity)
                .willBeApplied(false)
                .notAppliedReason(reason)
                .build();
    }

    private ValidateCampaignResponse buildValidateCampaignResponse(Campaign campaign,
                                                                   CampaignValidationContext context) {
        String finalMessage = context.isValid()
                ? "Campaign is valid and can be applied"
                : context.getMessageBuilder().toString().trim();

        return ValidateCampaignResponse.builder()
                .isValid(context.isValid())
                .message(finalMessage)
                .campaignResponse(campaignMapper.toResponse(campaign))
                .meetsDateRequirement(context.isMeetsDateRequirement())
                .meetsQuantityRequirement(context.isMeetsQuantityRequirement())
                .meetsFirstOrderRequirement(context.isMeetsFirstOrderRequirement())
                .meetsUsageLimitRequirement(context.isMeetsUsageLimitRequirement())
                .meetsPromoAbsenceRequirement(context.isMeetsPromoAbsenceRequirement())
                .userUsageCount(context.getUserUsageCount())
                .usageRemaining(context.getUsageRemaining())
                .freeQuantity(context.getFreeQuantity())
                .freeProductId(campaign.getFreeProduct().getId())
                .freeProductName(campaign.getFreeProduct().getName())
                .estimatedBonusValue(context.getEstimatedBonusValue())
                .userCanUse(context.isValid())
                .build();
    }

    private ApplyCampaignResponse buildSuccessResponse(Campaign campaign, CampaignUsage campaignUsage,
                                                       int freeQuantity, BigDecimal bonusValue) {
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

    private ApplyCampaignResponse buildFailureResponse(String message) {
        return ApplyCampaignResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    private Campaign findByCampaignById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Campaign not found with id: " + id));
    }

    private Campaign findCampaignByCode(String campaignCode) {
        return campaignRepository.findByCampaignCode(campaignCode)
                .orElseThrow(() -> new NotFoundException("Campaign not found with code: " + campaignCode));
    }

    private Product findProductById(Long productId, String productType) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
    }

    private void reactivateCampaignIfNeeded(
            Campaign campaign,
            UpdateCampaignRequest request,
            LocalDate oldValidFrom,
            LocalDate oldValidTo) {
        if (request.getValidFrom() != null || request.getValidTo() != null) {
            LocalDate newValidFrom = request.getValidFrom() != null ? request.getValidFrom() : oldValidFrom;
            LocalDate newValidTo = request.getValidTo() != null ? request.getValidTo() : oldValidTo;

            if (campaign.getCampaignStatus() == CampaignStatus.EXPIRED) {
                LocalDate now = LocalDate.now();
                if (!now.isBefore(newValidFrom) && !now.isAfter(newValidTo)) {
                    campaign.setCampaignStatus(CampaignStatus.ACTIVE);
                    log.info("Campaign {} reactivated due to valid date range", campaign.getId());
                }
            }
        }
    }

    private int getUserCampaignUsageCount(Long userId, String campaignCode) {
        return campaignUsageRepository.countByUserIdAndCampaignCampaignCode(userId, campaignCode);
    }

    private boolean isFirstOrder(Long userId) {
        return orderQueryService.getCompletedOrderCount(userId) == 0;
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
}