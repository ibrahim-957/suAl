package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.WarehouseMapper;
import com.delivery.SuAl.mapper.WarehouseStockMapper;
import com.delivery.SuAl.model.enums.WarehouseStatus;
import com.delivery.SuAl.model.request.warehouse.CreateWarehouseRequest;
import com.delivery.SuAl.model.request.warehouse.UpdateStockRequest;
import com.delivery.SuAl.model.response.warehouse.WarehouseResponse;
import com.delivery.SuAl.model.response.warehouse.WarehouseStockResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.WarehouseRepository;
import com.delivery.SuAl.repository.WarehouseStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseMapper warehouseMapper;
    private final WarehouseStockMapper warehouseStockMapper;

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest createWarehouseRequest) {
        log.info("Creating new warehouse with name: {}", createWarehouseRequest.getName());

        if (warehouseRepository.findByName(createWarehouseRequest.getName()).isPresent()) {
            throw new AlreadyExistsException("Warehouse already exists with name: " + createWarehouseRequest.getName());
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(createWarehouseRequest.getName());
        warehouse.setWarehouseStatus(WarehouseStatus.ACTIVE);

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        WarehouseResponse warehouseResponse = warehouseMapper.toResponse(savedWarehouse);
        warehouseResponse.setTotalProducts(0);
        warehouseResponse.setTotalStockCount(0);

        log.info("Created warehouse with ID: {}", warehouseResponse.getId());
        return warehouseResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        log.info("Getting warehouse with ID: {}", id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found with ID: " + id));

        WarehouseResponse response = warehouseMapper.toResponse(warehouse);

        List<WarehouseStock> stocks = warehouseStockRepository.findByWarehouseId(id);
        response.setTotalProducts(stocks.size());
        response.setTotalStockCount(calculateTotalStockCount(stocks));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WarehouseResponse> getAllWarehouse(Pageable pageable) {
        log.info("Getting all warehouse with pageable: {}", pageable);

        Page<Warehouse> warehousePage = warehouseRepository.findAll(pageable);

        List<Long> warehouseIds = warehousePage.getContent().stream()
                .map(Warehouse::getId)
                .collect(Collectors.toList());

        Map<Long, List<WarehouseStock>> stocksByWarehouse =
                warehouseStockRepository.findByWarehouseIdIn(warehouseIds).stream()
                        .collect(Collectors.groupingBy(stock -> stock.getWarehouse().getId()));

        List<WarehouseResponse> responses = warehousePage.getContent().stream()
                .map(warehouse -> {
                    WarehouseResponse warehouseResponse = warehouseMapper.toResponse(warehouse);
                    List<WarehouseStock> stocks = stocksByWarehouse.getOrDefault(
                            warehouse.getId(), Collections.emptyList());
                    warehouseResponse.setTotalProducts(stocks.size());
                    warehouseResponse.setTotalStockCount(calculateTotalStockCount(stocks));
                    return warehouseResponse;
                })
                .collect(Collectors.toList());
        return PageResponse.of(responses, warehousePage);
    }

    @Override
    @Transactional
    public void deleteWarehouseById(Long id) {
        warehouseRepository.deleteById(id);
        log.info("Deleted warehouse with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WarehouseStockResponse> getAllStockInWarehouse(Long warehouseId, Pageable pageable) {
        log.info("Getting all stock in warehouse with ID: {}", warehouseId);

        if (!warehouseRepository.existsById(warehouseId)) {
            throw new NotFoundException("Warehouse not found with ID: " + warehouseId);
        }

        Page<WarehouseStock> stockPage = warehouseStockRepository
                .findByWarehouseIdPageable(warehouseId, pageable);

        List<WarehouseStockResponse> responses = stockPage.getContent().stream()
                .map(warehouseStockMapper ::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, stockPage);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseStockResponse getStockByProductId(Long warehouseId, Long productId) {
        log.info("Getting stock for warehouse ID: {} and product ID: {}", warehouseId, productId);

        WarehouseStock stock = warehouseStockRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new NotFoundException(
                        "Stock not found for warehouse ID: " + warehouseId + " and product ID: " + productId));
        return warehouseStockMapper.toResponse(stock);
    }

    @Override
    @Transactional
    public WarehouseStockResponse updateStock(Long warehouseId, Long productId, UpdateStockRequest updateStockRequest) {
        log.info("Updating stock for warehouse ID: {} and product ID: {}", warehouseId, productId);

        WarehouseStock stock = warehouseStockRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new NotFoundException(
                        "Stock not found for warehouse ID: " + warehouseId + " and product ID: " + productId));

        if (updateStockRequest.getFullCount() != null) {
            stock.setFullCount(updateStockRequest.getFullCount());
        }
        if (updateStockRequest.getEmptyCount() != null) {
            stock.setEmptyCount(updateStockRequest.getEmptyCount());
        }
        if (updateStockRequest.getDamagedCount() != null) {
            stock.setDamagedCount(updateStockRequest.getDamagedCount());
        }

        stock.setLastRestocked(LocalDateTime.now());

        WarehouseStock updatedStock = warehouseStockRepository.save(stock);
        return warehouseStockMapper.toResponse(updatedStock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseStockResponse> getLowStockProducts(Long warehouseId) {
        log.info("Getting low stock stock products for warehouse ID: {}", warehouseId);

        List<WarehouseStock> stocks = warehouseStockRepository
                .findLowStockProductsByWarehouseId(warehouseId);
        return stocks.stream().map(warehouseStockMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseStockResponse> getOutOfStockProducts(Long warehouseId) {
        log.info("Getting out of  stock stock products for warehouse ID: {}", warehouseId);

        List<WarehouseStock> stocks = warehouseStockRepository
                .findOutOfStockProductsByWarehouseId(warehouseId);
        return stocks.stream().map(warehouseStockMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalInventoryCount(Long warehouseId) {
        log.info("Getting total inventory count for warehouse ID: {}", warehouseId);

        Long total = warehouseStockRepository.getTotalInventoryByWarehouse(warehouseId);
        return total != null ? total : 0L;
    }

    private int calculateTotalStockCount(List<WarehouseStock> stocks) {
        return stocks.stream()
                .mapToInt(s -> s.getFullCount() + s.getEmptyCount() + s.getDamagedCount())
                .sum();
    }
}
