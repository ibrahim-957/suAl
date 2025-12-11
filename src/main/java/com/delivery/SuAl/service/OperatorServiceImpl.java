package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.mapper.OperatorMapper;
import com.delivery.SuAl.mapper.OrderMapper;
import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.request.operation.CreateOperatorRequest;
import com.delivery.SuAl.model.request.operation.UpdateOperatorRequest;
import com.delivery.SuAl.model.response.operation.DriverResponse;
import com.delivery.SuAl.model.response.operation.OperatorResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.OperatorRepository;
import com.delivery.SuAl.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorServiceImpl implements OperatorService {
    private final OperatorRepository operatorRepository;
    private final OrderRepository orderRepository;
    private final OperatorMapper operatorMapper;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OperatorResponse createOperator(CreateOperatorRequest createOperatorRequest) {
        log.info("Creating new operator with email: {}", createOperatorRequest.getEmail());

        if (operatorRepository.findByEmail(createOperatorRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        if (operatorRepository.findByPhoneNumber(createOperatorRequest.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Phone number already exists");
        }

        Operator operator = operatorMapper.toEntity(createOperatorRequest);
        Operator savedOperator = operatorRepository.save(operator);

        log.info("Operator created: {}", savedOperator);
        return operatorMapper.toResponse(savedOperator);
    }

    @Override
    @Transactional(readOnly = true)
    public OperatorResponse getOperatorById(Long id) {
        log.info("Getting operator with id: {}", id);

        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator with id " + id + " not found"));

        return operatorMapper.toResponse(operator);
    }

    @Override
    @Transactional
    public OperatorResponse updateOperator(Long id, UpdateOperatorRequest updateOperatorRequest) {
        log.info("Updating operator with id: {}", id);

        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator with id " + id + " not found"));

        if (updateOperatorRequest.getPhoneNumber() != null && !updateOperatorRequest.getPhoneNumber().equals(operator.getPhoneNumber())) {
            operatorRepository.findByPhoneNumber(updateOperatorRequest.getPhoneNumber()).ifPresent(existing -> {
                throw new RuntimeException("Operator already exists with phone number: " + updateOperatorRequest.getPhoneNumber());
            });
        }

        if (updateOperatorRequest.getEmail() != null && !updateOperatorRequest.getEmail().equals(operator.getEmail())) {
            operatorRepository.findByEmail(updateOperatorRequest.getEmail()).ifPresent(existing -> {
                throw new RuntimeException("Operator already exists with email: " + updateOperatorRequest.getEmail());
            });
        }

        operatorMapper.updateEntityFromRequest(updateOperatorRequest, operator);
        Operator updatedOperator = operatorRepository.save(operator);
        log.info("Operator updated: {}", updatedOperator);
        return operatorMapper.toResponse(updatedOperator);
    }

    @Override
    @Transactional
    public void deleteOperator(Long id) {
        operatorRepository.deleteById(id);
        log.info("Operator deleted: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OperatorResponse> getAllOperators(Pageable pageable) {
        log.info("Getting all operators with page: {}", pageable);
        Page<Operator> operatorPage = operatorRepository.findAll(pageable);
        List<OperatorResponse> responses = operatorMapper.toResponseList(operatorPage.getContent());
        return PageResponse.of(responses, operatorPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getPendingOrders(Pageable pageable) {
        log.info("Getting pending orders with page: {}", pageable);
        Page<Order> orderPage = orderRepository.findByOrderStatus(OrderStatus.PENDING, pageable);

        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrdersForManagement(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);

        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, orderPage);
    }

    @Override
    public List<DriverResponse> getAvailableDrivers() {
        return List.of();
    }
}
