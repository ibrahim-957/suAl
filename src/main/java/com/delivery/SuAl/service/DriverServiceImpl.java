package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.mapper.DriverMapper;
import com.delivery.SuAl.model.DriverStatus;
import com.delivery.SuAl.model.OrderStatus;
import com.delivery.SuAl.model.request.operation.CreateDriverRequest;
import com.delivery.SuAl.model.request.operation.UpdateDriverRequest;
import com.delivery.SuAl.model.response.operation.DriverResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.DriverRepository;
import com.delivery.SuAl.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    @Transactional
    public DriverResponse createDriver(CreateDriverRequest createDriverRequest) {
        log.info("Creating new driver with phone number {}", createDriverRequest.getPhoneNumber());
        if (driverRepository.findByPhoneNumber(createDriverRequest.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Driver already exists with phone number " + createDriverRequest.getPhoneNumber());
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
                .orElseThrow(() -> new RuntimeException("Driver with id " + id + " not found"));

        DriverResponse driverResponse = driverMapper.toResponse(driver);
        enrichWithAvailability(driverResponse, id);

        return driverResponse;
    }

    @Override
    @Transactional
    public DriverResponse updateDriver(Long id, UpdateDriverRequest updateDriverRequest) {
        log.info("Updating driver with id {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver with id " + id + " not found"));

        if (updateDriverRequest.getPhoneNumber() != null && !updateDriverRequest.getPhoneNumber().equals(driver.getPhoneNumber())) {
            driverRepository.findByPhoneNumber(updateDriverRequest.getPhoneNumber()).ifPresent(existing -> {
                throw new RuntimeException("Driver already exists with phone number: " + updateDriverRequest.getPhoneNumber());
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
        driverRepository.deleteById(id);
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

    private void enrichWithAvailability(DriverResponse driverResponse, Long driverId) {
        List<OrderStatus> activeStatuses = Arrays.asList(
                OrderStatus.PENDING,
                OrderStatus.APPROVED
        );
        boolean hasActiveOrders = orderRepository.existsByDriverIdAndOrderStatusIn(driverId, activeStatuses);

        boolean isAvailable = !hasActiveOrders && driverResponse.getDriverStatus() == DriverStatus.ACTIVE;
        driverResponse.setAvailable(isAvailable);
    }
}
