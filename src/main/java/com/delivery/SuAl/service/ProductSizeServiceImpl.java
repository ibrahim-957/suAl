package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.ProductSize;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.ProductSizeMapper;
import com.delivery.SuAl.model.request.product.CreateProductSizeRequest;
import com.delivery.SuAl.model.request.product.UpdateProductSizeRequest;
import com.delivery.SuAl.model.response.product.ProductSizeResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.ProductSizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductSizeServiceImpl implements ProductSizeService {
    private final ProductSizeRepository productSizeRepository;
    private final ProductSizeMapper productSizeMapper;

    @Override
    public ProductSizeResponse createProductSize(CreateProductSizeRequest request) {
        log.info("Creating product size with label: {}", request.getLabel());

        if (productSizeRepository.existsByLabel(request.getLabel())) {
            throw new AlreadyExistsException("Product size with label: " + request.getLabel() + " already exists");
        }
        ProductSize size = new ProductSize();
        size.setLabel(request.getLabel());
        ProductSize saved = productSizeRepository.save(size);
        return productSizeMapper.toResponse(saved);
    }

    @Override
    public ProductSizeResponse updateProductSize(Long id, UpdateProductSizeRequest request) {
        log.info("Updating product size with ID: {}", id);

        ProductSize size = productSizeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product size not found with id: " + id));

        if (request.getLabel() != null && !request.getLabel().equals(size.getLabel())) {
            if (productSizeRepository.existsByLabel(request.getLabel())) {
                throw new AlreadyExistsException("Product size already exists with label: " + request.getLabel());
            }
            size.setLabel(request.getLabel());
        }

        if (request.getIsActive() != null) {
            size.setIsActive(request.getIsActive());
        }

        ProductSize updated = productSizeRepository.save(size);
        log.info("Product size updated with ID: {}", id);
        return productSizeMapper.toResponse(updated);
    }

    @Override
    public ProductSizeResponse getById(Long id) {
        log.info("Getting product size with ID: {}", id);
        ProductSize size = productSizeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product size not found with id: " + id));
        return productSizeMapper.toResponse(size);
    }

    @Override
    public void deleteProductSize(Long id) {
        ProductSize size = productSizeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product size not found with id: " + id));
        size.setIsActive(false);
        productSizeRepository.save(size);
        log.info("Product size soft deleted with ID: {}", id);
    }

    @Override
    public PageResponse<ProductSizeResponse> getAllProducts(Pageable pageable) {
        log.info("Getting all active product sizes");

        Page<ProductSize> productSize = productSizeRepository.findAll(pageable);
        List<ProductSizeResponse> responses = productSize.getContent().stream()
                .map(productSizeMapper::toResponse)
                .toList();

        return PageResponse.of(responses, productSize);
    }
}
