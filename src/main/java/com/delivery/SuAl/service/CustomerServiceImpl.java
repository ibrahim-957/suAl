package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.CustomerMapper;
import com.delivery.SuAl.model.request.customer.CreateCustomerRequest;
import com.delivery.SuAl.model.request.customer.UpdateCustomerRequest;
import com.delivery.SuAl.model.response.customer.CustomerResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponse createCustomer(CreateCustomerRequest createCustomerRequest) {
        log.info("Creating new customer with phone number {}", createCustomerRequest.getPhoneNumber());
        Optional<Customer> existingCustomer = customerRepository.findByPhoneNumber(createCustomerRequest.getPhoneNumber());

        if (existingCustomer.isPresent()) {
            Customer customer = existingCustomer.get();
            log.info("Reactivating customer with phone number {}", createCustomerRequest.getPhoneNumber());
            customer.setFirstName(createCustomerRequest.getFirstName());
            customer.setLastName(createCustomerRequest.getLastName());
            customer.setIsActive(true);
            Customer savedCustomer = customerRepository.save(customer);
            log.info("Customer reactivated successfully with ID: {}", savedCustomer.getId());
            return customerMapper.toResponse(savedCustomer);
        }

        Customer customer = customerMapper.toEntity(createCustomerRequest);
        customer.setIsActive(true);
        Customer savedCustomer = customerRepository.save(customer);

        log.info("Customer created successfully with ID: {}", savedCustomer.getId());
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest updateCustomerRequest) {
        log.info("Updating customer with id {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id " + id));

        if (updateCustomerRequest.getPhoneNumber() != null &&
                !updateCustomerRequest.getPhoneNumber().equals(customer.getUser().getPhoneNumber()) &&
                customerRepository.existsByPhoneNumber(updateCustomerRequest.getPhoneNumber())) {
            throw new AlreadyExistsException("Phone number already exists: " + updateCustomerRequest.getPhoneNumber());
        }

        customerMapper.updateEntityFromRequest(updateCustomerRequest, customer);
        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer updated successfully with ID: {}", savedCustomer.getId());
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    public CustomerResponse getCustomerById(Long id) {
        log.info("Getting customer with id {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id " + id));
        return customerMapper.toResponse(customer);
    }

    @Override
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with id {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id " + id));
        customer.setIsActive(false);
        customerRepository.save(customer);
        log.info("Customer deleted successfully with id: {}", id);
    }

    @Override
    public PageResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        log.info("Getting all customers with page {}", pageable);
        Page<Customer> customers = customerRepository.findAll(pageable);

        List<CustomerResponse> responses = customers.getContent().stream()
                .map(customerMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, customers);
    }

    @Override
    public Customer getCustomerEntityById(Long id) {
        log.info("Fetching customer entity by id: {}", id);
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id " + id));
    }
}