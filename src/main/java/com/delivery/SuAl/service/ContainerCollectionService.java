package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.warehouse.CollectContainersRequest;
import com.delivery.SuAl.model.response.warehouse.ContainerCollectionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ContainerCollectionService {
    ContainerCollectionResponse collectContainers(CollectContainersRequest request, Long userId);

    Page<ContainerCollectionResponse> getCollectionsByWarehouse(Long warehouseId, Pageable pageable);

    List<ContainerCollectionResponse> getCollectionsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate);

    Integer getTotalEmptyContainers(Long warehouseId, Long productId);

    Integer getTotalDamagedContainers(Long warehouseId, Long productId);

    ContainerCollectionResponse getCollectionById(Long collectionId);
}
