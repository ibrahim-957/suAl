package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.operation.CreateOperatorRequest;
import com.delivery.SuAl.model.request.operation.UpdateOperatorRequest;
import com.delivery.SuAl.model.response.operation.OperatorResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OperatorService {
    OperatorResponse createOperator(CreateOperatorRequest createOperatorRequest);

    OperatorResponse getOperatorById(Long id);

    OperatorResponse updateOperator(Long id, UpdateOperatorRequest updateOperatorRequest);

    void deleteOperator(Long id);

    PageResponse<OperatorResponse> getAllOperators(Pageable pageable);

}
