package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Category;
import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.ProductSize;
import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.UnauthorizedOperationException;
import com.delivery.SuAl.mapper.ProductMapper;
import com.delivery.SuAl.model.request.product.CreateProductRequest;
import com.delivery.SuAl.model.request.product.UpdateProductRequest;
import com.delivery.SuAl.model.response.product.ProductResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CategoryRepository;
import com.delivery.SuAl.repository.CompanyRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.ProductSizeRepository;
import com.delivery.SuAl.repository.WarehouseRepository;
import com.delivery.SuAl.repository.WarehouseStockRepository;
import com.delivery.SuAl.security.OperatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;
    private final ProductSizeRepository productSizeRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductMapper productMapper;
    private final ImageUploadService imageUploadService;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, MultipartFile image) {
        log.info("Creating new product with name: {}", request.getName());

        if (OperatorContext.isSupplierOperator()) {
            Long operatorCompanyId = OperatorContext.getCurrentCompanyId();
            if (!operatorCompanyId.equals(request.getCompanyId())) {
                throw new UnauthorizedOperationException(
                        "Supplier operators can only create products for their own company (ID: " + operatorCompanyId + ")"
                );
            }
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + request.getCompanyId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + request.getCategoryId()));

        ProductSize size = productSizeRepository.findById(request.getSizeId())  // NEW
                .orElseThrow(() -> new NotFoundException("Size not found with id: " + request.getSizeId()));

        if (productRepository.existsByCompanyIdAndNameAndSizeId(
                request.getCompanyId(), request.getName(), request.getSizeId())) {
            throw new AlreadyExistsException("Product already exists with this name and size for this company");
        }

        String imageUrl = imageUploadService.uploadImageForProduct(image);

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setCompany(company);
        product.setSize(size);
        product.setImageUrl(imageUrl);
        product.setSellPrice(request.getSellPrice());

        Product savedProduct = productRepository.save(product);

        WarehouseStock warehouseStock = new WarehouseStock();
        warehouseStock.setWarehouse(warehouse);
        warehouseStock.setProduct(savedProduct);
        warehouseStock.setFullCount(request.getInitialFullCount());
        warehouseStock.setEmptyCount(defaultValue(request.getInitialEmptyCount(), 0));
        warehouseStock.setDamagedCount(defaultValue(request.getInitialDamagedCount(), 0));
        warehouseStock.setMinimumStockAlert(defaultValue(request.getMinimumStockAlert(), 10));
        warehouseStock.setLastRestocked(LocalDateTime.now());
        warehouseStockRepository.save(warehouseStock);

        log.info("Product created successfully with ID: {}", savedProduct.getId());
        return productMapper.toResponse(savedProduct);  // no more enrichWithPriceData
    }

    @Override
    public ProductResponse getProductByID(Long id) {
        log.info("Getting product by ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        if (OperatorContext.isSupplierOperator()) {
            Long operatorCompanyId = OperatorContext.getCurrentCompanyId();
            if (!product.getCompany().getId().equals(operatorCompanyId)) {
                throw new UnauthorizedOperationException(
                        "You don't have permission to view this product."
                );
            }
        }

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request, MultipartFile image) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        if (OperatorContext.isSupplierOperator()) {
            Long operatorCompanyId = OperatorContext.getCurrentCompanyId();
            if (!product.getCompany().getId().equals(operatorCompanyId)) {
                throw new UnauthorizedOperationException(
                        "You don't have permission to update this product."
                );
            }
            if (request.getCompanyId() != null && !request.getCompanyId().equals(operatorCompanyId)) {
                throw new UnauthorizedOperationException(
                        "Supplier operators cannot change the company of a product"
                );
            }
        }


        if (request.getCompanyId() != null && !request.getCompanyId().equals(product.getCompany().getId())) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new NotFoundException("Company not found with ID: " + request.getCompanyId()));
            product.setCompany(company);
        }

        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found with ID: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getSizeId() != null && !request.getSizeId().equals(product.getSize().getId())) {
            ProductSize size = productSizeRepository.findById(request.getSizeId())
                    .orElseThrow(() -> new NotFoundException("Size not found with ID: " + request.getSizeId()));
            product.setSize(size);
        }

        if (request.getSellPrice() != null) {
            product.setSellPrice(request.getSellPrice());
        }

        productMapper.updateEntityFromRequest(request, product);

        if (image != null && !image.isEmpty()) {
            if (product.getImageUrl() != null) {
                try {
                    imageUploadService.deleteImage(product.getImageUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete old image, continuing with update", e);
                }
            }
            product.setImageUrl(imageUploadService.uploadImageForProduct(image));
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID: {}", updatedProduct.getId());
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    public void deleteProductByID(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        if (OperatorContext.isSupplierOperator()){
            Long operatorCompanyId = OperatorContext.getCurrentCompanyId();
            if (!product.getCompany().getId().equals(operatorCompanyId)) {
                throw new UnauthorizedOperationException(
                        "You don't have permission to delete this product. It belongs to a different company."
                );
            }
        }

        if (product.getImageUrl() != null) {
            try {
                imageUploadService.deleteImage(product.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete product image from Cloudinary", e);
            }
        }
        productRepository.deleteById(id);
        log.info("Product deleted successfully with ID: {}", id);
    }

    @Override
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Getting all products with page: {}", pageable);

        Page<Product> productPage;
        if (OperatorContext.isSupplierOperator()) {
            Long companyId = OperatorContext.getCurrentCompanyId();
            productPage = productRepository.findByCompanyId(companyId, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        List<ProductResponse> responses = productPage.getContent().stream()
                .map(productMapper::toResponse)
                .toList();

        return PageResponse.of(responses, productPage);
    }

    private <T> T defaultValue(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}