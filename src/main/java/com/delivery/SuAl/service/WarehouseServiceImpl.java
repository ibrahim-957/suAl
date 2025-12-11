package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.mapper.WarehouseMapper;
import com.delivery.SuAl.mapper.WarehouseStockMapper;
import com.delivery.SuAl.model.WarehouseStatus;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
            throw new RuntimeException("Warehouse already exists");
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
                .orElseThrow(() -> new RuntimeException("Warehouse not found with ID: " + id));

        WarehouseResponse response = warehouseMapper.toResponse(warehouse);

        List<WarehouseStock> stocks = warehouseStockRepository.findByWarehouseId(id);
        response.setTotalProducts(stocks.size());
        response.setTotalStockCount(stocks.stream()
                .mapToInt(s -> s.getFullCount() + s.getEmptyCount() + s.getDamagedCount())
                .sum());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WarehouseResponse> getAllWarehouse(Pageable pageable) {
        log.info("Getting all warehouse with pageable: {}", pageable);

        Page<Warehouse> warehousePage = warehouseRepository.findAll(pageable);

        List<WarehouseResponse> responses = warehousePage.getContent().stream()
                .map(warehouse -> {
                    WarehouseResponse warehouseResponse = warehouseMapper.toResponse(warehouse);
                    List<WarehouseStock> stocks = warehouseStockRepository.findByWarehouseId(warehouse.getId());
                    warehouseResponse.setTotalProducts(stocks.size());
                    warehouseResponse.setTotalStockCount(stocks.stream()
                            .mapToInt(s -> s.getFullCount() + s.getEmptyCount() + s.getDamagedCount())
                            .sum());
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
            throw new RuntimeException("Warehouse not found with ID: " + warehouseId);
        }

        List<WarehouseStock> stocks = warehouseStockRepository.findByWarehouseId(warehouseId);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), stocks.size());

        List<WarehouseStockResponse> responses = stocks.subList(start, end).stream()
                .map(warehouseStockMapper::toResponse)
                .collect(Collectors.toList());

        Page<WarehouseStockResponse> page = new PageImpl<>(responses, pageable, stocks.size());
        return PageResponse.of(responses, page);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseStockResponse getStockByProductId(Long warehouseId, Long productId) {
        log.info("Getting stock for warehouse ID: {} and product ID: {}", warehouseId, productId);

        WarehouseStock stock = warehouseStockRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new RuntimeException(
                        "Stock not found for warehouse ID: " + warehouseId + " and product ID: " + productId));
        return warehouseStockMapper.toResponse(stock);
    }

    @Override
    @Transactional
    public WarehouseStockResponse updateStock(Long warehouseId, Long productId, UpdateStockRequest updateStockRequest) {
        log.info("Updating stock for warehouse ID: {} and product ID: {}", warehouseId, productId);

        WarehouseStock stock = warehouseStockRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new RuntimeException(
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

        List<WarehouseStock> stocks = warehouseStockRepository.findLowStockProducts();
        return stocks.stream()
                .filter(stock -> stock.getWarehouse().getId().equals(warehouseId))
                .map(warehouseStockMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseStockResponse> getOutOfStockProducts(Long warehouseId) {
        log.info("Getting out of  stock stock products for warehouse ID: {}", warehouseId);

        List<WarehouseStock> stocks = warehouseStockRepository.findOutOfStockProducts();

        return stocks.stream()
                .filter(stock -> stock.getWarehouse().getId().equals(warehouseId))
                .map(warehouseStockMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalInventoryCount(Long warehouseId) {
        log.info("Getting total inventory count for warehouse ID: {}", warehouseId);

        Long total = warehouseStockRepository.getTotalInventoryByWarehouse(warehouseId);
        return total != null ? total : 0L;
    }
}
