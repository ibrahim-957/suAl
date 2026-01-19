package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.marketing.ApplyCampaignRequest;
import com.delivery.SuAl.model.request.marketing.CreateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.GetEligibleCampaignsRequest;
import com.delivery.SuAl.model.request.marketing.UpdateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ValidateCampaignRequest;
import com.delivery.SuAl.model.request.marketing.ValidatePromoRequest;
import com.delivery.SuAl.model.response.marketing.ApplyCampaignResponse;
import com.delivery.SuAl.model.response.marketing.CampaignResponse;
import com.delivery.SuAl.model.response.marketing.EligibleCampaignsResponse;
import com.delivery.SuAl.model.response.marketing.ValidateCampaignResponse;
import com.delivery.SuAl.model.response.marketing.ValidatePromoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CampaignService {
    CampaignResponse createCampaign(CreateCampaignRequest request, MultipartFile image);

    CampaignResponse getCampaignById(Long id);

    Page<CampaignResponse> getCampaigns(Pageable pageable);

    CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request, MultipartFile image);

    void deleteCampaignById(Long id);

    ValidateCampaignResponse validateCampaign(ValidateCampaignRequest request);

    ApplyCampaignResponse applyCampaign(ApplyCampaignRequest request);

    EligibleCampaignsResponse getEligibleCampaigns(GetEligibleCampaignsRequest request);
}
