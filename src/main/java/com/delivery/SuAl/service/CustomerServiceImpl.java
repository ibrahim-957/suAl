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

        Optional<User> existingUser = userRepository.findByPhoneNumber(createCustomerRequest.getPhoneNumber());

        if (existingUser.isPresent()) {
            log.warn("User already exists with phone number: {}", createCustomerRequest.getPhoneNumber());
            throw new AlreadyExistsException(
                    "An account already exists with phone number: " + createCustomerRequest.getPhoneNumber() +
                            ". Please login instead."
            );
        }

        Optional<Customer> inactiveCustomer = customerRepository
                .findByPhoneNumber(createCustomerRequest.getPhoneNumber())
                .filter(c -> !c.getIsActive());

        if (inactiveCustomer.isPresent()) {
            log.info("Reactivating inactive customer with phone number {}", createCustomerRequest.getPhoneNumber());
            Customer customer = inactiveCustomer.get();

            customer.setFirstName(createCustomerRequest.getFirstName());
            customer.setLastName(createCustomerRequest.getLastName());
            customer.setIsActive(true);

            Customer savedCustomer = customerRepository.save(customer);

            User user = customer.getUser();
            if (user != null) {
                log.info("Reactivating existing user for customer {}", savedCustomer.getId());

                return AuthenticationResponse.builder()
                        .accessToken(jwtService.generateToken(user))
                        .refreshToken(jwtService.generateRefreshToken(user))
                        .tokenType("Bearer")
                        .expiresIn(3600L)
                        .userId(user.getId())
                        .build();
            } else {
                log.info("Creating new user for reactivated customer {}", savedCustomer.getId());
                AuthenticationResponse response = authenticationService.createUser(
                        createCustomerRequest.getPhoneNumber(),
                        createCustomerRequest.getPassword(),
                        UserRole.CUSTOMER,
                        savedCustomer.getId()
                );

                user = userRepository.findByPhoneNumber(createCustomerRequest.getPhoneNumber())
                        .orElseThrow(() -> new NotFoundException("User not found after creation"));

                savedCustomer.setUser(user);
                customerRepository.save(savedCustomer);

                return response;
            }
        }

        Customer customer = new Customer();
        customer.setFirstName(createCustomerRequest.getFirstName());
        customer.setLastName(createCustomerRequest.getLastName());
        customer.setIsActive(true);

        AuthenticationResponse response = authenticationService.createUser(
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

        if (customer.getUser() != null) {
            userRepository.delete(customer.getUser());
            log.info("Deleted User associated with Customer {}", id);
        }

        customerRepository.save(customer);
        log.info("Customer deleted successfully with id: {}", id);
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
}