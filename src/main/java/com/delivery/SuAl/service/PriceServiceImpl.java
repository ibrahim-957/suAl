package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Category;
import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.mapper.PriceMapper;
import com.delivery.SuAl.model.request.product.CreatePriceRequest;
import com.delivery.SuAl.model.request.product.UpdatePriceRequest;
import com.delivery.SuAl.model.response.product.PriceResponse;
import com.delivery.SuAl.repository.CategoryRepository;
import com.delivery.SuAl.repository.CompanyRepository;
import com.delivery.SuAl.repository.PriceRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {
    private final PriceRepository priceRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;
    private final PriceMapper priceMapper;
    @Override
    @Transactional
    public PriceResponse createPrice(CreatePriceRequest createPriceRequest) {
        Product product = productRepository.findById(createPriceRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Category category = categoryRepository.findById(createPriceRequest.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Company company = companyRepository.findById(createPriceRequest.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        Price price = priceMapper.toEntity(createPriceRequest);
        price.setProduct(product);
        price.setCategory(category);
        price.setCompany(company);

        Price savedPrice = priceRepository.save(price);

        log.info("Price created successfully");
        return priceMapper.toResponse(savedPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceResponse getPriceById(Long id) {
        log.info("Get price by id {}", id);

        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Price not found"));
        return priceMapper.toResponse(price);
    }

    @Override
    @Transactional
    public PriceResponse updatePrice(Long id, UpdatePriceRequest updatePriceRequest) {
        log.info("Update price by id {}", id);

        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Price not found"));

        priceMapper.updateEntityFromRequest(updatePriceRequest, price);
        Price updatedPrice = priceRepository.save(price);

        log.info("Price updated successfully");
        return priceMapper.toResponse(updatedPrice);
    }

    @Override
    @Transactional
    public void deleteOperator(Long id) {
        priceRepository.deleteById(id);
        log.info("Price deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public PriceResponse getPriceByProductId(Long productId) {
        log.info("Get price by product id {}", productId);

        Price price = priceRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Price not found"));

        return priceMapper.toResponse(price);
    }
}
