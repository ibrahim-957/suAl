package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.PurchaseInvoice;
import com.delivery.SuAl.entity.PurchaseInvoiceItem;
import com.delivery.SuAl.entity.StockBatch;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.exception.InvoiceAlreadyApprovedException;
import com.delivery.SuAl.exception.InvoiceNotEditableException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.PurchaseInvoiceItemMapper;
import com.delivery.SuAl.mapper.PurchaseInvoiceMapper;
import com.delivery.SuAl.model.enums.InvoiceStatus;
import com.delivery.SuAl.model.enums.MovementType;
import com.delivery.SuAl.model.enums.ReferenceType;
import com.delivery.SuAl.model.request.purchase.CreatePurchaseInvoiceRequest;
import com.delivery.SuAl.model.request.purchase.PurchaseInvoiceItemRequest;
import com.delivery.SuAl.model.request.purchase.UpdatePurchaseInvoiceRequest;
import com.delivery.SuAl.model.response.purchase.PurchaseInvoiceResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CompanyRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.PurchaseInvoiceItemRepository;
import com.delivery.SuAl.repository.PurchaseInvoiceRepository;
import com.delivery.SuAl.repository.StockBatchRepository;
import com.delivery.SuAl.repository.WarehouseRepository;
import com.delivery.SuAl.repository.WarehouseStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseInvoiceServiceImpl implements PurchaseInvoiceService {
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final PurchaseInvoiceItemRepository purchaseInvoiceItemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final CompanyRepository companyRepository;
    private final PurchaseInvoiceMapper purchaseInvoiceMapper;
    private final PurchaseInvoiceItemMapper purchaseInvoiceItemMapper;
    private final StockMovementService stockMovementService;

    @Override
    public PurchaseInvoiceResponse createInvoice(CreatePurchaseInvoiceRequest request, User createdBy) {
        log.info("Creating purchase invoice: {} by user: {}",
                request.getInvoiceNumber(), createdBy.getId());

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found: " + request.getWarehouseId()));

        Company supplier = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + request.getCompanyId()));

        PurchaseInvoice invoice = purchaseInvoiceMapper.toEntity(request);
        invoice.setWarehouse(warehouse);
        invoice.setCompany(supplier);
        invoice.setCreatedBy(createdBy);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setStatus(InvoiceStatus.DRAFT);

        List<PurchaseInvoiceItem> items = buildItems(request.getItems(), invoice);
        invoice.setItems(items);
        invoice.setTotalAmount(calculateTotal(items));
        invoice.setTotalDepositAmount(calculateTotalDeposit(items));

        PurchaseInvoice saved = purchaseInvoiceRepository.save(invoice);
        log.info("Purchase invoice created with ID: {} status: DRAFT", saved.getId());

        return purchaseInvoiceMapper.toResponse(
                purchaseInvoiceRepository.findByIdWithItems(saved.getId()).orElse(saved));
    }

    @Override
    public PurchaseInvoiceResponse getInvoiceById(Long id) {
        log.info("Getting purchase invoice by ID: {}", id);
        PurchaseInvoice invoice = purchaseInvoiceRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Purchase invoice not found: " + id));
        return purchaseInvoiceMapper.toResponse(invoice);
    }

    @Override
    public PurchaseInvoiceResponse updateInvoice(Long id, UpdatePurchaseInvoiceRequest request) {
        log.info("Updating purchase invoice ID: {}", id);

        PurchaseInvoice invoice = purchaseInvoiceRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new InvoiceNotEditableException(
                    "Invoice ID " + id + " cannot be edited — status is: " + invoice.getStatus()
                            + ". APPROVED invoices require a reversal invoice.");
        }

        purchaseInvoiceMapper.updateEntityFromRequest(request, invoice);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            purchaseInvoiceItemRepository.deleteByInvoiceId(id);
            List<PurchaseInvoiceItem> newItems = buildItems(request.getItems(), invoice);
            invoice.setItems(newItems);
            invoice.setTotalAmount(calculateTotal(newItems));
            invoice.setTotalDepositAmount(calculateTotalDeposit(newItems));
        }

        PurchaseInvoice updated = purchaseInvoiceRepository.save(invoice);
        log.info("Invoice ID: {} updated successfully", id);
        return purchaseInvoiceMapper.toResponse(updated);
    }

    @Override
    public PurchaseInvoiceResponse approveInvoice(Long id, User approvedBy) {
        log.info("Approving purchase invoice ID: {} by: {}", id, approvedBy);

        PurchaseInvoice invoice = purchaseInvoiceRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));

        if (invoice.isApproved()) {
            throw new InvoiceAlreadyApprovedException(
                    "Invoice ID " + id + " is already approved");
        }
        if (invoice.isCancelled()){
            throw new InvoiceNotEditableException(
                    "Invoice ID " + id + " is cancelled and cannot be approved");
        }

        invoice.approve(approvedBy);

        Warehouse warehouse = invoice.getWarehouse();
        List<Long> productIds = invoice.getItems().stream()
                .map(item -> item.getProduct().getId())
                .toList();

        Map<Long, WarehouseStock> stockMap = warehouseStockRepository
                .findByProductIdsWithLock(productIds).stream()
                .collect(Collectors.toMap(s->s.getProduct().getId(), s->s));

        List<StockBatch> batches = new ArrayList<>();

        for (PurchaseInvoiceItem item : invoice.getItems()) {
            Long productId = item.getProduct().getId();

            StockBatch batch = new StockBatch();
            batch.setProduct(item.getProduct());
            batch.setWarehouse(warehouse);
            batch.setInvoiceItem(item);
            batch.setInitialQuantity(item.getQuantity());
            batch.setRemainingQuantity(item.getQuantity());
            batch.setPurchasePrice(item.getPurchasePrice());
            batches.add(batch);

            WarehouseStock stock = stockMap.get(productId);
            if (stock != null){
                stock.setFullCount(stock.getFullCount() + item.getQuantity());
                stock.setLastRestocked(LocalDateTime.now());
            } else {
                log.warn("WarehouseStock missing for product {} in warehouse {} — creating",
                        productId, warehouse.getId());
                WarehouseStock newStock = new WarehouseStock();
                newStock.setProduct(item.getProduct());
                newStock.setWarehouse(warehouse);
                newStock.setFullCount(item.getQuantity());
                newStock.setEmptyCount(0);
                newStock.setDamagedCount(0);
                newStock.setMinimumStockAlert(10);
                newStock.setLastRestocked(LocalDateTime.now());
                stockMap.put(productId, newStock);
            }

            stockMovementService.record(
                    productId, warehouse.getId(), MovementType.PURCHASE,
                    ReferenceType.PURCHASE_INVOICE, invoice.getId(), item.getQuantity(),
                    "Stock received via invoice: " + invoice.getInvoiceNumber());

            log.debug("Approved item: product {} qty {} — batch + movement + stock updated",
                    productId, item.getQuantity());
        }

        stockBatchRepository.saveAll(batches);
        warehouseStockRepository.saveAll(new ArrayList<>(stockMap.values()));
        purchaseInvoiceRepository.save(invoice);

        log.info("Invoice ID: {} approved. {} batches created.", id, batches.size());
        return purchaseInvoiceMapper.toResponse(invoice);
    }

    @Override
    public PurchaseInvoiceResponse cancelInvoice(Long id) {
        log.info("Cancelling purchase invoice ID: {}", id);

        PurchaseInvoice invoice = purchaseInvoiceRepository
                .findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));

        try {
            invoice.cancel();
        } catch (IllegalStateException e) {
            throw new InvoiceNotEditableException(e.getMessage());
        }

        purchaseInvoiceRepository.save(invoice);
        log.info("Invoice ID: {} cancelled.", id);
        return purchaseInvoiceMapper.toResponse(invoice);
    }

    @Override
    public PageResponse<PurchaseInvoiceResponse> getAllInvoices(Pageable pageable) {
        Page<PurchaseInvoice>  page = purchaseInvoiceRepository.findAll(pageable);
        List<PurchaseInvoiceResponse> responses = page.getContent().stream()
                .map(purchaseInvoiceMapper::toResponse)
                .toList();
        return PageResponse.of(responses, page);
    }

    private List<PurchaseInvoiceItem> buildItems(List<PurchaseInvoiceItemRequest> itemRequests,
                                                 PurchaseInvoice invoice) {
        List<Long> productIds = itemRequests.stream()
                .map(PurchaseInvoiceItemRequest::getProductId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<String> missing = productIds.stream()
                .filter(pid -> !productMap.containsKey(pid))
                .map(String::valueOf)
                .toList();

        if (!missing.isEmpty()) {
            throw new NotFoundException("Product not found: " + missing);
        }

        return itemRequests.stream().map(req -> {
                    PurchaseInvoiceItem item = purchaseInvoiceItemMapper.toEntity(req);
                    item.setProduct(productMap.get(req.getProductId()));
                    item.setInvoice(invoice);
                    item.setLineTotal(req.getPurchasePrice()
                            .multiply(BigDecimal.valueOf(req.getQuantity())));
                    return item;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateTotal(List<PurchaseInvoiceItem> items) {
        return items.stream()
                .map(PurchaseInvoiceItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalDeposit(List<PurchaseInvoiceItem> items) {
        return items.stream()
                .filter(i -> i.getDepositUnitAmount() != null)
                .map(i -> i.getDepositUnitAmount()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
