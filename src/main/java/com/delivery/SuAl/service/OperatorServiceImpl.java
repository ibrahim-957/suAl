package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.entity.Operator;
import com.delivery.SuAl.entity.User;
import com.delivery.SuAl.exception.AlreadyExistsException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.mapper.OperatorMapper;
import com.delivery.SuAl.model.enums.OperatorStatus;
import com.delivery.SuAl.model.enums.OperatorType;
import com.delivery.SuAl.model.enums.UserRole;
import com.delivery.SuAl.model.request.operation.CreateOperatorRequest;
import com.delivery.SuAl.model.request.operation.UpdateOperatorRequest;
import com.delivery.SuAl.model.response.auth.AuthenticationResponse;
import com.delivery.SuAl.model.response.operation.OperatorResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CompanyRepository;
import com.delivery.SuAl.repository.OperatorRepository;
import com.delivery.SuAl.repository.UserRepository;
import com.delivery.SuAl.security.OperatorInfo;
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
    private final OperatorMapper operatorMapper;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public AuthenticationResponse createOperator(CreateOperatorRequest createOperatorRequest) {
        log.info("Creating new operator with email: {}", createOperatorRequest.getEmail());

        if (operatorRepository.findByEmail(createOperatorRequest.getEmail()).isPresent()) {
            throw new AlreadyExistsException("Email already exists: " + createOperatorRequest.getEmail());
        }

        if (operatorRepository.findByPhoneNumber(createOperatorRequest.getPhoneNumber()).isPresent()) {
            throw new AlreadyExistsException("Phone number already exists: " + createOperatorRequest.getPhoneNumber());
        }

        Operator operator = new Operator();
        operator.setFirstName(createOperatorRequest.getFirstName());
        operator.setLastName(createOperatorRequest.getLastName());
        operator.setOperatorStatus(OperatorStatus.ACTIVE);

        OperatorType operatorType = createOperatorRequest.getOperatorType() != null
                ? createOperatorRequest.getOperatorType()
                : OperatorType.SYSTEM;
        operator.setOperatorType(operatorType);

        if (operatorType == OperatorType.SUPPLIER) {
            if (createOperatorRequest.getCompanyId() == null) {
                throw new IllegalArgumentException("Company Id is required for SUPPLIER operators");
            }

            Company company = companyRepository.findById(createOperatorRequest.getCompanyId())
                    .orElseThrow(() -> new NotFoundException("Company not found with Id: " + createOperatorRequest.getCompanyId()));
            operator.setCompany(company);
            log.info("Creating SUPPLIER operator for company: {}", company.getName());
        } else {
            log.info("Creating SYSTEM operator (can access all companies)");
        }

        AuthenticationResponse response = authenticationService.createUser(
                createOperatorRequest.getEmail(),
                createOperatorRequest.getPhoneNumber(),
                createOperatorRequest.getPassword(),
                UserRole.OPERATOR,
                null
        );

        User user = userRepository.findByEmail(createOperatorRequest.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found after creation"));

        operator.setUser(user);

        Operator savedOperator = operatorRepository.save(operator);
        log.info("Operator entity created with ID: {}", savedOperator.getId());

        user.setTargetId(savedOperator.getId());
        userRepository.save(user);

        log.info("Operator created successfully with ID: {} and linked to User", savedOperator.getId());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OperatorResponse getOperatorById(Long id) {
        log.info("Getting operator with id: {}", id);

        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Operator with id " + id + " not found"));

        return operatorMapper.toResponse(operator);
    }

    @Override
    @Transactional
    public OperatorResponse updateOperator(Long id, UpdateOperatorRequest updateOperatorRequest) {
        log.info("Updating operator with id: {}", id);

        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Operator with id " + id + " not found"));

        if (updateOperatorRequest.getPhoneNumber() != null &&
                !updateOperatorRequest.getPhoneNumber().equals(operator.getUser().getPhoneNumber())) {

            if (userRepository.existsByPhoneNumber(updateOperatorRequest.getPhoneNumber())) {
                throw new AlreadyExistsException("Operator already exists with phone number: " + updateOperatorRequest.getPhoneNumber());
            }

            operator.getUser().setPhoneNumber(updateOperatorRequest.getPhoneNumber());
            userRepository.save(operator.getUser());
        }

        if (updateOperatorRequest.getEmail() != null &&
                !updateOperatorRequest.getEmail().equals(operator.getUser().getEmail())) {

            if (userRepository.existsByEmail(updateOperatorRequest.getEmail())) {
                throw new AlreadyExistsException("Operator already exists with email: " + updateOperatorRequest.getEmail());
            }

            operator.getUser().setEmail(updateOperatorRequest.getEmail());
            userRepository.save(operator.getUser());
        }

        if (updateOperatorRequest.getOperatorType() != null) {
            operator.setOperatorType(updateOperatorRequest.getOperatorType());
        }

        if (updateOperatorRequest.getCompanyId() != null) {
            if (operator.getOperatorType() == OperatorType.SYSTEM) {
                log.warn("Attempting to set company for SYSTEM operator - company will be set to null");
                operator.setCompany(null);
            } else {
                Company company = companyRepository.findById(updateOperatorRequest.getCompanyId())
                        .orElseThrow(() -> new NotFoundException("Company not found with id: " + updateOperatorRequest.getCompanyId()));
                operator.setCompany(company);
            }
        }

        operatorMapper.updateEntityFromRequest(updateOperatorRequest, operator);
        Operator updatedOperator = operatorRepository.save(operator);

        log.info("Operator updated with id: {}", updatedOperator.getId());
        return operatorMapper.toResponse(updatedOperator);
    }

    @Override
    @Transactional
    public void deleteOperator(Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Operator with id " + id + " not found"));

        operator.setOperatorStatus(OperatorStatus.INACTIVE);

        if (operator.getUser() != null) {
            userRepository.delete(operator.getUser());
            log.info("Deleted User associated with Operator {}", id);
        }

        operatorRepository.save(operator);
        log.info("Operator deleted with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OperatorResponse> getAllOperators(Pageable pageable) {
        log.info("Getting all operators with page: {}", pageable);
        Page<Operator> operatorPage = operatorRepository.findAll(pageable);

        List<OperatorResponse> responses = operatorPage.getContent().stream()
                .map(operatorMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(responses, operatorPage);
    }

    @Override
    public OperatorInfo getOperatorInfo(String email) {
        log.info("Getting operator info for email: {}", email);
        return operatorRepository.findByUserEmail(email)
                .map(operator -> OperatorInfo.builder()
                        .operatorId(operator.getId())
                        .companyId(operator.getCompany() != null ? operator.getCompany().getId() : null)
                        .operatorType(operator.getOperatorType())
                        .email(email)
                        .firstName(operator.getFirstName())
                        .lastName(operator.getLastName())
                        .build())
                .orElse(null);
    }
}