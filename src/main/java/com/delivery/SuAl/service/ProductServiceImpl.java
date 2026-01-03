package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Category;
import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.entity.Warehouse;
import com.delivery.SuAl.entity.WarehouseStock;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.ProductMapper;
import com.delivery.SuAl.model.request.product.CreatePriceRequest;
import com.delivery.SuAl.model.request.product.CreateProductRequest;
import com.delivery.SuAl.model.request.product.UpdatePriceRequest;
import com.delivery.SuAl.model.request.product.UpdateProductRequest;
import com.delivery.SuAl.model.response.product.PriceResponse;
import com.delivery.SuAl.model.response.product.ProductResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CategoryRepository;
import com.delivery.SuAl.repository.CompanyRepository;
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
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;
    private final PriceService priceService;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating new product with name: {}", request.getName());

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + request.getCompanyId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + request.getCategoryId()));

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setCompany(company);

        Product savedProduct = productRepository.save(product);

        if (request.getBuyPrice() != null || request.getSellPrice() != null) {
            CreatePriceRequest priceRequest = new CreatePriceRequest();
            priceRequest.setProductId(savedProduct.getId());
            priceRequest.setCategoryId(category.getId());
            priceRequest.setCompanyId(company.getId());
            priceRequest.setSellPrice(request.getSellPrice());
            priceRequest.setBuyPrice(request.getBuyPrice());

            priceService.createPrice(priceRequest);
        }

        WarehouseStock warehouseStock = new WarehouseStock();
        warehouseStock.setWarehouse(warehouse);
        warehouseStock.setProduct(savedProduct);
        warehouseStock.setFullCount(request.getInitialFullCount());
        warehouseStock.setEmptyCount(defaultValue(request.getInitialEmptyCount(), 0));
        warehouseStock.setDamagedCount(defaultValue(request.getInitialDamagedCount(), 0));
        warehouseStock.setMinimumStockAlert(defaultValue(request.getMinimumStockAlert(), 10));
        warehouseStock.setLastRestocked(LocalDateTime.now());

        warehouseStockRepository.save(warehouseStock);

        ProductResponse response = productMapper.toResponse(savedProduct);
        enrichWithPriceData(response, savedProduct.getId());

        log.info("Product created successfully with ID: {} and price", savedProduct.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductByID(Long id) {
        log.info("Getting product by ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        ProductResponse response = productMapper.toResponse(product);
        enrichWithPriceData(response, product.getId());
        return response;
    }

    @Override
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with name: {}", request.getName());

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

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
        if (request.getName() != null && !request.getName().equals(product.getName())) {
            productRepository.findByNameAndCompanyId(request.getName(), product.getCompany().getId())
                    .ifPresent(existing -> {
                        throw new AlreadyExistsException("Product already exists with name: " + request.getName() + " for this company");
                    });
        }

        productMapper.updateEntityFromRequest(request, product);
        Product updatedProduct = productRepository.save(product);

        if (request.getBuyPrice() != null || request.getSellPrice() != null) {
            try {
                PriceResponse existingPrice = priceService.getPriceByProductId(updatedProduct.getId());
                UpdatePriceRequest priceRequest = new UpdatePriceRequest();
                priceRequest.setBuyPrice(request.getBuyPrice());
                priceRequest.setSellPrice(request.getSellPrice());
                priceService.updatePrice(existingPrice.getId(), priceRequest);
            } catch (RuntimeException e) {
                CreatePriceRequest priceRequest = new CreatePriceRequest();
                priceRequest.setProductId(updatedProduct.getId());
                priceRequest.setCategoryId(updatedProduct.getCategory().getId());
                priceRequest.setCompanyId(updatedProduct.getCompany().getId());
                priceRequest.setSellPrice(request.getSellPrice());
                priceRequest.setBuyPrice(request.getBuyPrice());
                priceService.createPrice(priceRequest);
            }
        }

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

        List<Product> products = productPage.getContent();

        List<Long> productIds = products
                .stream()
                .map(Product::getId)
                .toList();


        Map<Long, PriceResponse> priceMap = priceService.getPricesByProductIds(productIds);
        List<ProductResponse> responses = products.stream()
                .map(product -> {
                    ProductResponse response = productMapper.toResponse(product);

                    PriceResponse price = priceMap.get(product.getId());
                    if (price != null) {
                        response.setSellPrice(price.getSellPrice());
                        response.setBuyPrice(price.getBuyPrice());
                    }
                    return response;
                })
                .toList();

        return PageResponse.of(responses, productPage);
    }

    private void enrichWithPriceData(ProductResponse productResponse, Long productId) {
        try {
            PriceResponse priceResponse = priceService.getPriceByProductId(productId);
            productResponse.setSellPrice(priceResponse.getSellPrice());
            productResponse.setBuyPrice(priceResponse.getBuyPrice());
        } catch (RuntimeException ignored) {
        }
    }

    private <T> T defaultValue(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
