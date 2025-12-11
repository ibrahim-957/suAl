package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Category;
import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.mapper.ProductMapper;
import com.delivery.SuAl.model.request.product.CreateProductRequest;
import com.delivery.SuAl.model.request.product.UpdateProductRequest;
import com.delivery.SuAl.model.response.product.ProductResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CategoryRepository;
import com.delivery.SuAl.repository.CompanyRepository;
import com.delivery.SuAl.repository.PriceRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.repository.WarehouseRepository;
import com.delivery.SuAl.repository.WarehouseStockRepository;
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
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;
    private final PriceRepository  priceRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating new product with name: {}", request.getName());

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(()-> new RuntimeException("Company not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(()-> new RuntimeException("Category not found"));

        if (productRepository.findByNameAndCompanyId(request.getName(), request.getCompanyId()).isPresent()) {
            throw new RuntimeException("Product already exists");
        }

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setCompany(company);

        Product savedProduct = productRepository.save(product);

        Price price = new Price();
        price.setProduct(savedProduct);
        price.setCategory(category);
        price.setCompany(company);
        price.setBuyPrice(request.getBuyPrice());
        price.setSellPrice(request.getSellPrice());

        Price savedPrice = priceRepository.save(price);

        WarehouseStock warehouseStock = new WarehouseStock();
        warehouseStock.setWarehouse(warehouse);
        warehouseStock.setProduct(savedProduct);
        warehouseStock.setCategory(category);
        warehouseStock.setCompany(company);
        warehouseStock.setFullCount(request.getInitialFullCount());
        warehouseStock.setEmptyCount(request.getInitialEmptyCount() != null ? request.getInitialEmptyCount() : 0);
        warehouseStock.setDamagedCount(request.getInitialDamagedCount() != null ? request.getInitialDamagedCount() : 0);
        warehouseStock.setMinimumStockAlert(request.getMinimumStockAlert() != null ? request.getMinimumStockAlert() : 10);
        warehouseStock.setLastRestocked(LocalDateTime.now());

        warehouseStockRepository.save(warehouseStock);

        ProductResponse response = productMapper.toResponse(savedProduct);
        response.setBuyPrice(savedPrice.getBuyPrice());
        response.setSellPrice(savedPrice.getSellPrice());

        log.info("Product created successfully with ID: {} and price", savedProduct.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductByID(Long id) {
        log.info("Getting product by ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Product not found"));

        ProductResponse response = productMapper.toResponse(product);
        enrichWithPriceData(response, id);
        return response;
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with name: {}", request.getName());

        Product product = productRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Product not found"));

        if (request.getCompanyId() != null && !request.getCompanyId().equals(product.getCompany().getId())) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found with ID: " + request.getCompanyId()));
            product.setCompany(company);
        }

        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with ID: " + request.getCategoryId()));
            product.setCategory(category);
        }
        if (request.getName() != null && !request.getName().equals(product.getName())) {
            productRepository.findByNameAndCompanyId(request.getName(), product.getCompany().getId())
                    .ifPresent(existing -> {
                        throw new RuntimeException("Product already exists with name: " + request.getName() + " for this company");
                    });
        }

        productMapper.updateEntityFromRequest(request, product);
        Product updatedProduct = productRepository.save(product);

        ProductResponse response = productMapper.toResponse(updatedProduct);
        enrichWithPriceData(response, id);

        log.info("Product updated successfully with ID: {} and price", updatedProduct.getId());
        return response;
    }

    @Override
    public void deleteProductByID(Long id) {
        productRepository.deleteById(id);
        log.info("Product deleted successfully with ID: {}", id);
    }

    @Override
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Getting all products with page: {}", pageable);

        Page<Product> productPage = productRepository.findAll(pageable);

        List<ProductResponse> responses = productPage.getContent().stream()
                .map(product -> {
                    ProductResponse response = productMapper.toResponse(product);
                    enrichWithPriceData(response, product.getId());
                    return response;
                })
                .collect(Collectors.toList());

        return PageResponse.of(responses, productPage);
    }

    private void enrichWithPriceData(ProductResponse productResponse, Long productId) {
        priceRepository.findByProductId(productId).ifPresent(price -> {
            productResponse.setSellPrice(price.getSellPrice());
            productResponse.setBuyPrice(price.getBuyPrice());
        });
    }
}
