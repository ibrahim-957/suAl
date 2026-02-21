package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.transfer.CreateWarehouseTransferRequest;
import com.delivery.SuAl.model.request.transfer.UpdateWarehouseTransferRequest;
import com.delivery.SuAl.model.response.transfer.WarehouseTransferResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface WarehouseTransferService {
    WarehouseTransferResponse createTransfer(CreateWarehouseTransferRequest request);

    WarehouseTransferResponse getTransferById(Long id);

    WarehouseTransferResponse updateTransfer(Long id, UpdateWarehouseTransferRequest request);

    WarehouseTransferResponse completeTransfer(Long id);

    WarehouseTransferResponse cancelTransfer(Long id);

    PageResponse<WarehouseTransferResponse> getAllTransfers(Pageable pageable);
}
