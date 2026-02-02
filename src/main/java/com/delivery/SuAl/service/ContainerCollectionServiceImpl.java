package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Admin;
import com.delivery.SuAl.entity.ContainerCollection;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.ContainerCollectionMapper;
import com.delivery.SuAl.model.request.warehouse.CollectContainersRequest;
import com.delivery.SuAl.model.response.warehouse.ContainerCollectionResponse;
import com.delivery.SuAl.repository.AdminRepository;
import com.delivery.SuAl.repository.ContainerCollectionRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.UserRepository;
import com.delivery.SuAl.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerCollectionServiceImpl implements ContainerCollectionService {

    private final ContainerCollectionRepository containerCollectionRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ContainerCollectionMapper mapper;

    @Override
    @Transactional
    public ContainerCollectionResponse collectContainers(CollectContainersRequest request, Long userId) {
        log.info("Collecting containers from warehouse: {} for product: {}",
                request.getWarehouseId(), request.getProductId());

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new NotFoundException(
                        "Warehouse not found with ID: " + request.getWarehouseId()));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException(
                        "Product not found with ID: " + request.getProductId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "User not found with ID: " + userId));

        if ((request.getEmptyContainers() == null || request.getEmptyContainers() == 0) &&
                (request.getDamagedContainers() == null || request.getDamagedContainers() == 0)) {
            throw new IllegalArgumentException(
                    "At least one container (empty or damaged) must be collected");
        }

        ContainerCollection collection = ContainerCollection.builder()
                .warehouse(warehouse)
                .product(product)
                .emptyContainers(request.getEmptyContainers() != null ? request.getEmptyContainers() : 0)
                .damagedContainers(request.getDamagedContainers() != null ? request.getDamagedContainers() : 0)
                .collectedBy(user)
                .collectionDateTime(LocalDateTime.now())
                .notes(request.getNotes())
                .build();

        ContainerCollection savedCollection = containerCollectionRepository.save(collection);

        log.info("Container collection saved with ID: {}. Total: {} containers",
                savedCollection.getId(), savedCollection.getTotalCollected());

        Admin admin = adminRepository.findById(user.getTargetId())
                .orElse(null);

        return mapper.toResponse(savedCollection, admin);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContainerCollectionResponse> getCollectionsByWarehouse(Long warehouseId, Pageable pageable) {
        log.info("Fetching container collections for warehouse: {}", warehouseId);

        Page<ContainerCollection> collections = containerCollectionRepository
                .findByWarehouseId(warehouseId, pageable);

        return collections.map(collection -> {
            Admin admin = adminRepository.findById(collection.getCollectedBy().getTargetId())
                    .orElse(null);
            return mapper.toResponse(collection, admin);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContainerCollectionResponse> getCollectionsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching container collections between {} and {}", startDate, endDate);

        List<ContainerCollection> collections = containerCollectionRepository
                .findByCollectionDateTimeBetween(startDate, endDate);

        return collections.stream()
                .map(collection -> {
                    Admin admin = adminRepository.findById(collection.getCollectedBy().getTargetId())
                            .orElse(null);
                    return mapper.toResponse(collection, admin);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalEmptyContainers(Long warehouseId, Long productId) {
        log.info("Calculating total empty containers for warehouse: {} and product: {}",
                warehouseId, productId);

        Integer total = containerCollectionRepository
                .getTotalEmptyContainersByWarehouseAndProduct(warehouseId, productId);

        return total != null ? total : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalDamagedContainers(Long warehouseId, Long productId) {
        log.info("Calculating total damaged containers for warehouse: {} and product: {}",
                warehouseId, productId);

        Integer total = containerCollectionRepository
                .getTotalDamagedContainersByWarehouseAndProduct(warehouseId, productId);

        return total != null ? total : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public ContainerCollectionResponse getCollectionById(Long collectionId) {
        log.info("Fetching container collection with ID: {}", collectionId);

        ContainerCollection collection = containerCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new NotFoundException(
                        "Container collection not found with ID: " + collectionId));

        Admin admin = adminRepository.findById(collection.getCollectedBy().getTargetId())
                .orElse(null);

        return mapper.toResponse(collection, admin);
    }
}
