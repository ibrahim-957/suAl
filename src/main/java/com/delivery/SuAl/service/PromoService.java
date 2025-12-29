package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.marketing.ApplyPromoRequest;
import com.delivery.SuAl.model.request.marketing.CreatePromoRequest;
import com.delivery.SuAl.model.request.marketing.UpdatePromoRequest;
import com.delivery.SuAl.model.request.marketing.ValidatePromoRequest;
import com.delivery.SuAl.model.response.marketing.ApplyPromoResponse;
import com.delivery.SuAl.model.response.marketing.PromoResponse;
import com.delivery.SuAl.model.response.marketing.ValidatePromoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PromoService {
    PromoResponse createPromo(CreatePromoRequest request);

    PromoResponse getPromoById(Long id);

    Page<PromoResponse> getAllPromos(Pageable pageable);

    PromoResponse updatePromo(Long id, UpdatePromoRequest request);

    void deletePromoById(Long id);

    ValidatePromoResponse validatePromo(ValidatePromoRequest request);

    ApplyPromoResponse applyPromo(ApplyPromoRequest request);
}
