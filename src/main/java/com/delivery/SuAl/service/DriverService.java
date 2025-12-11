package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.operation.CreateDriverRequest;
import com.delivery.SuAl.model.request.operation.UpdateDriverRequest;
import com.delivery.SuAl.model.response.operation.DriverResponse;
import com.delivery.SuAl.model.response.order.OrderResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface DriverService {
    DriverResponse createDriver(CreateDriverRequest createDriverRequest);

    DriverResponse getDriverById(Long id);

    DriverResponse updateDriver(Long id, UpdateDriverRequest updateDriverRequest);

    void deleteDriver(Long id);

    PageResponse<DriverResponse> getAllDrivers(Pageable pageable);

    PageResponse<OrderResponse> getMyAssignedOrders(Long driverId, Pageable pageable);
}
