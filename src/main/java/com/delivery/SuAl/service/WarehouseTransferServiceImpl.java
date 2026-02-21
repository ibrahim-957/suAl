package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.entity.WarehouseTransfer;
import com.delivery.SuAl.entity.WarehouseTransferItem;
import com.delivery.SuAl.exception.InsufficientStockException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.TransferNotEditableException;
import com.delivery.SuAl.exception.TransferSameWarehouseException;
import com.delivery.SuAl.mapper.WarehouseTransferItemMapper;
import com.delivery.SuAl.mapper.WarehouseTransferMapper;
import com.delivery.SuAl.model.enums.MovementType;
import com.delivery.SuAl.model.enums.ReferenceType;
import com.delivery.SuAl.model.enums.TransferStatus;
import com.delivery.SuAl.model.request.transfer.CreateWarehouseTransferRequest;
import com.delivery.SuAl.model.request.transfer.UpdateWarehouseTransferRequest;
import com.delivery.SuAl.model.request.transfer.WarehouseTransferItemRequest;
import com.delivery.SuAl.model.response.transfer.WarehouseTransferResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.WarehouseRepository;
import com.delivery.SuAl.repository.WarehouseStockRepository;
import com.delivery.SuAl.repository.WarehouseTransferItemRepository;
import com.delivery.SuAl.repository.WarehouseTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseTransferServiceImpl implements WarehouseTransferService{
    private final WarehouseTransferRepository transferRepository;
    private final WarehouseTransferItemRepository transferItemRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final WarehouseTransferMapper transferMapper;
    private final WarehouseTransferItemMapper transferItemMapper;
    private final StockMovementService stockMovementService;

    @Override
    public WarehouseTransferResponse createTransfer(CreateWarehouseTransferRequest request) {
        log.info("Creating warehouse transfer: {} -> {}",
                request.getFromWarehouseId(), request.getToWarehouseId());

        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new TransferSameWarehouseException(
                    "Source and destination warehouse cannot be the same. ID: "
                    + request.getFromWarehouseId());
        }

        Warehouse fromWarehouse = warehouseRepository.findById(request.getFromWarehouseId())
                .orElseThrow(() -> new NotFoundException(
                        "Source warehouse not found: "  + request.getFromWarehouseId()));

        Warehouse toWarehouse = warehouseRepository.findById(request.getToWarehouseId())
                .orElseThrow(() -> new NotFoundException(
                        "Destination warehouse not found: "  + request.getToWarehouseId()));

        WarehouseTransfer transfer = transferMapper.toEntity(request);
        transfer.setFromWarehouse(fromWarehouse);
        transfer.setToWarehouse(toWarehouse);
        transfer.setStatus(TransferStatus.PENDING);

        List<WarehouseTransferItem> items = buildItems(request.getItems(), transfer);
        transfer.setItems(items);

        WarehouseTransfer saved = transferRepository.save(transfer);
        log.info("Transfer created with ID: {} status: PENDING", saved.getId());

        return transferMapper.toResponse(
                transferRepository.findByIdWithItems(saved.getId()).orElse(saved)
        );
    }

    @Override
    public WarehouseTransferResponse getTransferById(Long id) {
        log.info("Getting warehouse transfer ID: {}", id);
        WarehouseTransfer transfer = transferRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Transfer not found with id: " + id));
        return transferMapper.toResponse(transfer);
    }

    @Override
    public WarehouseTransferResponse updateTransfer(Long id, UpdateWarehouseTransferRequest request) {
        log.info("Updating warehouse transfer ID: {}", id);

        WarehouseTransfer transfer = transferRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Transfer not found with id: " + id));

        if (transfer.getStatus() == TransferStatus.PENDING) {
            throw new TransferNotEditableException(
                    "Transfer ID " + id + " cannot be edited - status is: "
                    + transfer.getStatus());
        }

        transferMapper.updateEntityFromRequest(request, transfer);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            transferItemRepository.deleteByTransferId(id);
            List<WarehouseTransferItem> newItem = buildItems(request.getItems(), transfer);
            transfer.setItems(newItem);
        }

        WarehouseTransfer updated =transferRepository.save(transfer);
        log.info("Transfer ID: {} updated", id);
        return transferMapper.toResponse(updated);
    }

    @Override
    public WarehouseTransferResponse completeTransfer(Long id) {
        log.info("Completing warehouse transfer ID: {}", id);

        WarehouseTransfer transfer = transferRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Transfer not found: " + id));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new TransferNotEditableException(
                    "Transfer ID " + id + " cannot be completed — status is: "
                            + transfer.getStatus());
        }

        Warehouse fromWarehouse = transfer.getFromWarehouse();
        Warehouse toWarehouse = transfer.getToWarehouse();

        List<Long> productIds = transfer.getItems().stream()
                .map(item -> item.getProduct().getId())
                .toList();

        Map<Long, WarehouseStock> fromStockMap = warehouseStockRepository
                .findByProductIdsWithLock(productIds)
                .stream()
                .filter(s -> s.getWarehouse().getId().equals(fromWarehouse.getId()))
                .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));

        List<String> errors = new ArrayList<>();
        for (WarehouseTransferItem item : transfer.getItems()) {
            Long productId = item.getProduct().getId();
            WarehouseStock fromStock = fromStockMap.get(productId);
            if (fromStock == null) {
                errors.add("Product " + item.getProduct().getName()
                        + " not found in source warehouse");
                continue;
            }
            if (fromStock.getFullCount() < item.getQuantity()) {
                errors.add(String.format(
                        "Insufficient stock for '%s'. Available: %d, Required: %d",
                        item.getProduct().getName(),
                        fromStock.getFullCount(),
                        item.getQuantity()));
            }
        }
        if (!errors.isEmpty()) {
            throw new InsufficientStockException(
                    "Transfer validation failed: " + String.join("; ", errors));
        }

        Map<Long, WarehouseStock> toStockMap = warehouseStockRepository
                .findByWarehouseIdIn(productIds)
                .stream()
                .filter(s -> s.getWarehouse().getId().equals(toWarehouse.getId()))
                .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));

        Map<Long, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<WarehouseStock> stocksToSave = new ArrayList<>();

        for (WarehouseTransferItem item : transfer.getItems()) {
            Long productId = item.getProduct().getId();
            Integer quantity = item.getQuantity();

            WarehouseStock fromStock = fromStockMap.get(productId);
            fromStock.setFullCount(fromStock.getFullCount() - quantity);
            stocksToSave.add(fromStock);

            WarehouseStock toStock = toStockMap.get(productId);
            if (toStock != null) {
                toStock.setFullCount(toStock.getFullCount() + quantity);
                stocksToSave.add(toStock);
            } else {
                log.info("Creating WarehouseStock for product {} in destination warehouse {}",
                        productId, toWarehouse.getId());
                WarehouseStock newStock = new WarehouseStock();
                newStock.setProduct(productMap.get(productId));
                newStock.setWarehouse(toWarehouse);
                newStock.setFullCount(quantity);
                newStock.setEmptyCount(0);
                newStock.setDamagedCount(0);
                newStock.setMinimumStockAlert(10);
                newStock.setLastRestocked(LocalDateTime.now());
                stocksToSave.add(newStock);
            }

            stockMovementService.record(
                    productId, fromWarehouse.getId(),
                    MovementType.TRANSFER_OUT,
                    ReferenceType.TRANSFER,
                    transfer.getId(),
                    quantity,
                    "Transfer out to warehouse: " + toWarehouse.getName()
            );

            stockMovementService.record(
                    productId, toWarehouse.getId(),
                    MovementType.TRANSFER_IN,
                    ReferenceType.TRANSFER,
                    transfer.getId(),
                    quantity,
                    "Transfer in from warehouse: " + fromWarehouse.getName()
            );

            log.debug("Transfer item: product {} qty {} moved {} → {}",
                    productId, quantity, fromWarehouse.getName(), toWarehouse.getName());
        }

        warehouseStockRepository.saveAll(stocksToSave);

        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        log.info("Transfer ID: {} completed. {} products moved from {} to {}",
                id, transfer.getItems().size(),
                fromWarehouse.getName(), toWarehouse.getName());

        return transferMapper.toResponse(transfer);
    }

    @Override
    public WarehouseTransferResponse cancelTransfer(Long id) {
        log.info("Cancelling warehouse transfer ID: {}", id);

        WarehouseTransfer transfer = transferRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Transfer not found: " + id));

        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            throw new TransferNotEditableException(
                    "Transfer ID " + id + " is COMPLETED and cannot be cancelled. "
                            + "Create a reverse transfer to undo it.");
        }
        if (transfer.getStatus() == TransferStatus.CANCELLED) {
            throw new TransferNotEditableException(
                    "Transfer ID " + id + " is already cancelled.");
        }

        transfer.setStatus(TransferStatus.CANCELLED);
        transferRepository.save(transfer);

        log.info("Transfer ID: {} cancelled (was PENDING — no stock to reverse)", id);
        return transferMapper.toResponse(transfer);
    }

    @Override
    public PageResponse<WarehouseTransferResponse> getAllTransfers(Pageable pageable) {
        log.info("Getting all transfers, page: {}", pageable);
        Page<WarehouseTransfer> page = transferRepository.findAll(pageable);
        List<WarehouseTransferResponse> responses = page.getContent().stream()
                .map(transferMapper::toResponse)
                .toList();
        return PageResponse.of(responses, page);
    }

    private List<WarehouseTransferItem> buildItems(List<WarehouseTransferItemRequest> requests,
                                                   WarehouseTransfer transfer) {
        List<Long> productIds = requests.stream()
                .map(WarehouseTransferItemRequest::getProductId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<String> missing = productIds.stream()
                .filter(pid -> !productMap.containsKey(pid))
                .map(String::valueOf)
                .toList();

        if (!missing.isEmpty()) {
            throw new NotFoundException("Product not found: " + missing);
        }

        return requests.stream().map(req -> {
            WarehouseTransferItem item = transferItemMapper.toEntity(req);
            item.setProduct(productMap.get(req.getProductId()));
            item.setTransfer(transfer);
            return item;
        }).collect(Collectors.toList());
    }
}
