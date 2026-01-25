package com.delivery.SuAl.service;


import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.model.request.customer.CreateCustomerRequest;
import com.delivery.SuAl.model.request.customer.UpdateCustomerRequest;
import com.delivery.SuAl.model.response.customer.CustomerResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    CustomerResponse createCustomer(CreateCustomerRequest createCustomerRequest);

    CustomerResponse updateCustomer(Long id, UpdateCustomerRequest updateCustomerRequest);

    CustomerResponse getCustomerById(Long id);

    void deleteCustomer(Long id);

    PageResponse<CustomerResponse> getAllCustomers(Pageable pageable);

    Customer getCustomerEntityById(Long id);
}
