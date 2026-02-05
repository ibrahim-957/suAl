package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Customer;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.CustomerMapper;
import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.model.request.customer.CreateCustomerRequest;
import com.delivery.SuAl.model.request.customer.UpdateCustomerRequest;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
import com.delivery.SuAl.model.response.customer.CustomerResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CustomerRepository;
import com.delivery.SuAl.repository.UserRepository;
import com.delivery.SuAl.security.JwtService;
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
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthenticationResponse createCustomer(CreateCustomerRequest createCustomerRequest) {
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

        AuthenticationResponse response = authenticationService.createUser(
                null,
                createCustomerRequest.getPhoneNumber(),
                createCustomerRequest.getPassword(),
                UserRole.CUSTOMER,
                null
        );

        User user = userRepository.findByPhoneNumber(createCustomerRequest.getPhoneNumber())
                .orElseThrow(() -> new NotFoundException("User not found after creation"));

        customer.setUser(user);

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer entity created with ID: {}", savedCustomer.getId());

        user.setTargetId(savedCustomer.getId());
        userRepository.save(user);

        log.info("Customer created successfully with ID: {} and linked to User", savedCustomer.getId());

        return response;
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

    private AuthenticationResponse reactivateCustomer(Customer customer, CreateCustomerRequest request) {
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setIsActive(true);

        if (customer.getUser() != null) {
            log.info("User still exists for inactive customer, reactivating with existing credentials");

            customerRepository.save(customer);

            User user = customer.getUser();

            return AuthenticationResponse.builder()
                    .accessToken(jwtService.generateToken(user))
                    .refreshToken(jwtService.generateRefreshToken(user))
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .userId(user.getId())
                    .role(UserRole.CUSTOMER)
                    .build();
        } else {
            log.info("Creating new user for reactivated customer {}", customer.getId());

            AuthenticationResponse response = authenticationService.createUser(
                    null,
                    request.getPhoneNumber(),
                    request.getPassword(),
                    UserRole.CUSTOMER,
                    null
            );

            User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new NotFoundException("User not found after creation"));

            customer.setUser(user);
            customerRepository.save(customer);

            user.setTargetId(customer.getId());
            userRepository.save(user);

            log.info("Customer reactivated successfully with ID: {}", customer.getId());

            return response;
        }
    }
}