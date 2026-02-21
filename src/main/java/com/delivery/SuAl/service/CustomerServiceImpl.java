package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.CustomerMapper;
import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.model.request.customer.CreateCustomerRequest;
import com.delivery.SuAl.model.request.customer.UpdateCustomerRequest;
import com.delivery.SuAl.model.response.customer.CustomerResponse;
import com.delivery.SuAl.model.response.wrapper.ApiResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CustomerRepository;
import com.delivery.SuAl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ApiResponse<String> createCustomer(CreateCustomerRequest createCustomerRequest) {
        log.info("Creating new customer with phone number {}", createCustomerRequest.getPhoneNumber());

        Optional<Customer> existingCustomer = customerRepository
                .findByPhoneNumberIncludingInactive(createCustomerRequest.getPhoneNumber());

        if (existingCustomer.isPresent()) {
            Customer customer = existingCustomer.get();

            if (!customer.getIsActive()) {
                log.info("Reactivating existing INACTIVE customer with ID: {}", customer.getId());
                return reactivateCustomer(customer, createCustomerRequest);
            } else {
                throw new AlreadyExistsException(
                        "An account already exists with phone number: " + createCustomerRequest.getPhoneNumber() +
                                ". Please login instead."
                );
            }
        }

        Customer customer = new Customer();
        customer.setFirstName(createCustomerRequest.getFirstName());
        customer.setLastName(createCustomerRequest.getLastName());
        customer.setIsActive(true);

        authenticationService.createUser(
                null,
                createCustomerRequest.getPhoneNumber(),
                null,
                UserRole.CUSTOMER
        );

        User user = userRepository.findByPhoneNumber(createCustomerRequest.getPhoneNumber())
                .orElseThrow(() -> new NotFoundException("User not found after creation"));

        customer.setUser(user);
        Customer savedCustomer = customerRepository.save(customer);

        userRepository.save(user);

        log.info("Customer created successfully with ID: {}", savedCustomer.getId());

        return ApiResponse.<String>builder()
                .success(true)
                .message("Account created successfully")
                .data("Please login via OTP to continue")
                .build();
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest updateCustomerRequest) {
        log.info("Updating customer with id {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id " + id));

        if (updateCustomerRequest.getPhoneNumber() != null &&
                !updateCustomerRequest.getPhoneNumber().equals(customer.getUser().getPhoneNumber())) {

            if (userRepository.existsByPhoneNumber(updateCustomerRequest.getPhoneNumber())) {
                throw new AlreadyExistsException("Phone number already exists: " + updateCustomerRequest.getPhoneNumber());
            }

            customer.getUser().setPhoneNumber(updateCustomerRequest.getPhoneNumber());
            userRepository.save(customer.getUser());
        }

        customerMapper.updateEntityFromRequest(updateCustomerRequest, customer);
        Customer savedCustomer = customerRepository.save(customer);

        log.info("Customer updated successfully with ID: {}", savedCustomer.getId());
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        log.info("Getting customer with id {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id " + id));
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with id {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id " + id));

        customer.setIsActive(false);
        customerRepository.save(customer);

        if (customer.getUser() != null) {
            User user = customer.getUser();
            customer.setUser(null);
            customerRepository.save(customer);
            userRepository.delete(user);
            log.info("Deleted User associated with Customer {}", id);
        }

        log.info("Customer soft deleted with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        log.info("Getting all customers with page {}", pageable);
        Page<Customer> customers = customerRepository.findAll(pageable);

        List<CustomerResponse> responses = customers.getContent().stream()
                .map(customerMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, customers);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerEntityById(Long id) {
        log.info("Fetching customer entity by id: {}", id);
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with id " + id));
    }

    private ApiResponse<String> reactivateCustomer(Customer customer, CreateCustomerRequest request) {
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setIsActive(true);

        if (customer.getUser() == null) {
            authenticationService.createUser(
                    null,
                    request.getPhoneNumber(),
                    null,
                    UserRole.CUSTOMER
            );

            User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new NotFoundException("User not found after creation"));

            customer.setUser(user);
            customerRepository.save(customer);

            userRepository.save(user);
        } else {
            customerRepository.save(customer);
        }

        log.info("Customer reactivated successfully with ID: {}", customer.getId());

        return ApiResponse.<String>builder()
                .success(true)
                .message("Account reactivated successfully")
                .data("Please login via OTP to continue")
                .build();
    }
}