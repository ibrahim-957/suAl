package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Driver;
import com.delivery.SuAl.entity.Order;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.DriverMapper;
import com.delivery.SuAl.mapper.OrderMapper;
import com.delivery.SuAl.model.enums.DriverStatus;
import com.delivery.SuAl.model.enums.OrderStatus;
import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.model.request.operation.CreateDriverRequest;
import com.delivery.SuAl.model.request.operation.UpdateDriverRequest;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
import com.delivery.SuAl.model.response.operation.DriverResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.DriverRepository;
import com.delivery.SuAl.repository.OrderRepository;
import com.delivery.SuAl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AuthenticationResponse createDriver(CreateDriverRequest createDriverRequest) {
        log.info("Creating new driver with email {}", createDriverRequest.getEmail());

        if (driverRepository.findByEmail(createDriverRequest.getEmail()).isPresent()) {
            throw new AlreadyExistsException("Driver already exists with email " + createDriverRequest.getEmail());
        }

        if (driverRepository.findByPhoneNumber(createDriverRequest.getPhoneNumber()).isPresent()) {
            throw new AlreadyExistsException("Driver already exists with phone number " + createDriverRequest.getPhoneNumber());
        }

        Driver driver = new Driver();
        driver.setFirstName(createDriverRequest.getFirstName());
        driver.setLastName(createDriverRequest.getLastName());
        driver.setDriverStatus(DriverStatus.ACTIVE);

        AuthenticationResponse response = authenticationService.createUser(
                createDriverRequest.getEmail(),
                createDriverRequest.getPassword(),
                UserRole.DRIVER,
                null
        );

        User user = userRepository.findByEmail(createDriverRequest.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found with email " + createDriverRequest.getEmail()));

        driver.setUser(user);

        Driver savedDriver = driverRepository.save(driver);

        user.setTargetId(savedDriver.getId());
        userRepository.save(user);
        log.info("Driver created successfully with ID: {} and linked to User", savedDriver.getId());

        return response;
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

        if (updateDriverRequest.getPhoneNumber() != null &&
                !updateDriverRequest.getPhoneNumber().equals(driver.getUser().getPhoneNumber())) {

            if (userRepository.existsByPhoneNumber(updateDriverRequest.getPhoneNumber())) {
                throw new AlreadyExistsException("Driver already exists with phone number: " + updateDriverRequest.getPhoneNumber());
            }

            driver.getUser().setPhoneNumber(updateDriverRequest.getPhoneNumber());
            userRepository.save(driver.getUser());
        }

        if (updateDriverRequest.getEmail() != null &&
                !updateDriverRequest.getEmail().equals(driver.getUser().getEmail())) {

            if (userRepository.existsByEmail(updateDriverRequest.getEmail())) {
                throw new AlreadyExistsException("Driver already exists with email: " + updateDriverRequest.getEmail());
            }

            driver.getUser().setEmail(updateDriverRequest.getEmail());
            userRepository.save(driver.getUser());
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

        if (driver.getUser() != null) {
            userRepository.delete(driver.getUser());
            log.info("Deleted User associated with Driver {}", id);
        }

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
    public PageResponse<OrderResponse> getMyAssignedOrders(String email, Pageable pageable) {
        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Driver with email " + email + " not found"));
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "deliveryDate", "createdAt"));

        Page<Order> orderPage = orderRepository.findByDriverIdAndOrderStatus(
                driver.getId(), OrderStatus.APPROVED, sortedPageable);

        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, orderPage);
    }

    private void enrichWithAvailability(DriverResponse driverResponse, Long driverId) {
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.APPROVED
        );
        boolean hasActiveOrders = orderRepository.existsByDriverIdAndOrderStatusIn(driverId, activeStatuses);

        boolean isAvailable = !hasActiveOrders && driverResponse.getDriverStatus() == DriverStatus.ACTIVE;
        driverResponse.setAvailable(isAvailable);
    }
}