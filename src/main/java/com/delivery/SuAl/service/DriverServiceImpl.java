package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.DriverMapper;
import com.delivery.SuAl.mapper.OrderMapper;
import com.delivery.SuAl.model.enums.DriverStatus;
import com.delivery.SuAl.model.enums.OrderStatus;
import com.delivery.SuAl.model.request.operation.CreateDriverRequest;
import com.delivery.SuAl.model.request.operation.UpdateDriverRequest;
import com.delivery.SuAl.model.response.operation.DriverResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.DriverRepository;
import com.delivery.SuAl.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {
    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public DriverResponse createDriver(CreateDriverRequest createDriverRequest) {
        log.info("Creating new driver with phone number {}", createDriverRequest.getPhoneNumber());
        if (driverRepository.findByPhoneNumber(createDriverRequest.getPhoneNumber()).isPresent()) {
            throw new AlreadyExistsException("Driver already exists with phone number " + createDriverRequest.getPhoneNumber());
        }
        Driver driver = driverMapper.toEntity(createDriverRequest);
        Driver savedDriver = driverRepository.save(driver);
        DriverResponse driverResponse = driverMapper.toResponse(savedDriver);
        enrichWithAvailability(driverResponse, savedDriver.getId());

        log.info("Driver created successfully");
        return driverResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long id) {
        log.info("Fetching driver with id {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver with id " + id + " not found"));

        DriverResponse driverResponse = driverMapper.toResponse(driver);
        enrichWithAvailability(driverResponse, id);

        return driverResponse;
    }

    @Override
    @Transactional
    public DriverResponse updateDriver(Long id, UpdateDriverRequest updateDriverRequest) {
        log.info("Updating driver with id {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver with id " + id + " not found"));

        if (updateDriverRequest.getPhoneNumber() != null && !updateDriverRequest.getPhoneNumber().equals(driver.getPhoneNumber())) {
            driverRepository.findByPhoneNumber(updateDriverRequest.getPhoneNumber()).ifPresent(existing -> {
                throw new AlreadyExistsException("Driver already exists with phone number: " + updateDriverRequest.getPhoneNumber());
            });
        }

        driverMapper.updateEntityFromRequest(updateDriverRequest, driver);
        Driver updatedDriver = driverRepository.save(driver);

        DriverResponse driverResponse = driverMapper.toResponse(updatedDriver);
        enrichWithAvailability(driverResponse, id);

        log.info("Driver updated successfully");
        return driverResponse;
    }

    @Override
    @Transactional
    public void deleteDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver with id " + id + " not found"));

        driver.setDriverStatus(DriverStatus.INACTIVE);
        driverRepository.save(driver);
        log.info("Driver deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DriverResponse> getAllDrivers(Pageable pageable) {
        log.info("Fetching all drivers");

        Page<Driver> driverPage = driverRepository.findAll(pageable);

        List<DriverResponse> responses = driverPage.getContent().stream()
                .map(driver -> {
                    DriverResponse driverResponse = driverMapper.toResponse(driver);
                    enrichWithAvailability(driverResponse, driver.getId());
                    return driverResponse;
                })
                .collect(Collectors.toList());
        return PageResponse.of(responses, driverPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyAssignedOrders(Long driverId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "deliveryDate", "createdAt"));

        Page<Order> orderPage = orderRepository.findByDriverIdAndOrderStatus(
                driverId, OrderStatus.APPROVED, sortedPageable);

        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, orderPage);
    }

    private void enrichWithAvailability(DriverResponse driverResponse, Long driverId) {
        List<OrderStatus> activeStatuses = Arrays.asList(
                OrderStatus.APPROVED
        );
        boolean hasActiveOrders = orderRepository.existsByDriverIdAndOrderStatusIn(driverId, activeStatuses);

        boolean isAvailable = !hasActiveOrders && driverResponse.getDriverStatus() == DriverStatus.ACTIVE;
        driverResponse.setAvailable(isAvailable);
    }

}
