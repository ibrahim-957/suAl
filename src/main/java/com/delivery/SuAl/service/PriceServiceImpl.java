package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Price;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.PriceMapper;
import com.delivery.SuAl.model.request.product.CreatePriceRequest;
import com.delivery.SuAl.model.request.product.UpdatePriceRequest;
import com.delivery.SuAl.model.response.product.PriceResponse;
import com.delivery.SuAl.repository.PriceRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {
    private final PriceRepository priceRepository;
    private final ProductRepository productRepository;
    private final PriceMapper priceMapper;
    @Override
    @Transactional
    public PriceResponse createPrice(CreatePriceRequest createPriceRequest) {
        Product product = productRepository.findById(createPriceRequest.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id " + createPriceRequest.getProductId()));

        Price price = priceMapper.toEntity(createPriceRequest);
        price.setProduct(product);

        Price savedPrice = priceRepository.save(price);

        log.info("Price created successfully");
        return priceMapper.toResponse(savedPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceResponse getPriceById(Long id) {
        log.info("Get price by id {}", id);

        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Price not found with id " + id));
        return priceMapper.toResponse(price);
    }

    @Override
    @Transactional
    public PriceResponse updatePrice(Long id, UpdatePriceRequest updatePriceRequest) {
        log.info("Update price by id {}", id);

        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Price not found with id " + id));

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
                .orElseThrow(() -> new NotFoundException("Price not found with id " + productId));

        return priceMapper.toResponse(price);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, PriceResponse> getPricesByProductIds(List<Long> productIds) {
        return priceRepository.findAllByProductIdIn(productIds)
                .stream()
                .map(priceMapper::toResponse)
                .collect(Collectors.toMap(p -> p.getProductId(), p -> p));
    }
}
