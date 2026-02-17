package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.AffordablePackage;
import com.delivery.SuAl.entity.AffordablePackageProduct;
import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.entity.CustomerPackageOrder;
import com.delivery.SuAl.entity.Product;
import com.delivery.SuAl.exception.BusinessRuleViolationException;
import com.delivery.SuAl.exception.NotFoundException;
import com.delivery.SuAl.exception.UnauthorizedOperationException;
import com.delivery.SuAl.mapper.AffordablePackageMapper;
import com.delivery.SuAl.model.request.affordablepackage.CreateAffordablePackageRequest;
import com.delivery.SuAl.model.request.affordablepackage.PackageProductRequest;
import com.delivery.SuAl.model.request.affordablepackage.UpdateAffordablePackageRequest;
import com.delivery.SuAl.model.response.affordablepackage.AffordablePackageResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.AffordablePackageProductRepository;
import com.delivery.SuAl.repository.AffordablePackageRepository;
import com.delivery.SuAl.repository.CompanyRepository;
import com.delivery.SuAl.repository.CustomerPackageOrderRepository;
import com.delivery.SuAl.repository.ProductRepository;
import com.delivery.SuAl.security.OperatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AffordablePackageServiceImpl implements AffordablePackageService {
    private final AffordablePackageRepository affordablePackageRepository;
    private final AffordablePackageProductRepository affordablePackageProductRepository;
    private final CustomerPackageOrderRepository customerPackageOrderRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final AffordablePackageMapper affordablePackageMapper;

    @Override
    @Transactional
    public AffordablePackageResponse createPackage(CreateAffordablePackageRequest request) {
        log.info("Creating affordable package: {}", request.getName());

        Company company = null;
        if (OperatorContext.isSupplierOperator()) {
            Long operatorCompanyId = OperatorContext.getCurrentCompanyId();
            company = findCompanyById(operatorCompanyId);

            validateProductsAccess(request.getProducts(), operatorCompanyId);

            log.info("Supplier operator creating package for company: {}", operatorCompanyId);
        } else {
            if (request.getCompanyId() != null) {
                company = findCompanyById(request.getCompanyId());
            }
            log.info("System operator/admin creating package");
        }

        AffordablePackage affordablePackage = new AffordablePackage();
        affordablePackage.setName(request.getName());
        affordablePackage.setDescription(request.getDescription());
        affordablePackage.setTotalPrice(request.getTotalPrice());
        affordablePackage.setMaxFrequency(request.getMaxFrequency());
        affordablePackage.setIsActive(true);
        affordablePackage.setCompany(company);

        AffordablePackage savedPackage = affordablePackageRepository.save(affordablePackage);

        List<AffordablePackageProduct> packageProducts = createPackageProducts(savedPackage, request.getProducts());
        affordablePackageProductRepository.saveAll(packageProducts);
        savedPackage.getPackageProducts().addAll(packageProducts);

        log.info("Package created successfully: {}", savedPackage.getId());

        return affordablePackageMapper.toResponse(savedPackage);
    }

    @Override
    @Transactional
    public AffordablePackageResponse updatePackage(Long packageId, UpdateAffordablePackageRequest request) {
        log.info("Updating package: {}", packageId);

        AffordablePackage affordablePackage = findPackageById(packageId);
        validatePackageAccess(affordablePackage);

        if (request.getName() != null) {
            affordablePackage.setName(request.getName());
        }
        if (request.getDescription() != null) {
            affordablePackage.setDescription(request.getDescription());
        }
        if (request.getTotalPrice() != null) {
            affordablePackage.setTotalPrice(request.getTotalPrice());
        }
        if (request.getMaxFrequency() != null) {
            validateMaxFrequencyUpdate(affordablePackage, request.getMaxFrequency());
            affordablePackage.setMaxFrequency(request.getMaxFrequency());
        }
        if (request.getIsActive() != null) {
            affordablePackage.setIsActive(request.getIsActive());
        }

        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            if (OperatorContext.isSupplierOperator()) {
                Long operatorCompanyId = OperatorContext.getCurrentCompanyId();
                validateProductsAccess(request.getProducts(), operatorCompanyId);
            }

            affordablePackageProductRepository.deleteAll(affordablePackage.getPackageProducts());
            affordablePackage.getPackageProducts().clear();

            List<AffordablePackageProduct> newProducts = createPackageProducts(affordablePackage, request.getProducts());
            affordablePackageProductRepository.saveAll(newProducts);
            affordablePackage.getPackageProducts().addAll(newProducts);
        }

        AffordablePackage updated = affordablePackageRepository.save(affordablePackage);
        log.info("Package updated successfully: {}", packageId);

        return affordablePackageMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public AffordablePackageResponse getPackageById(Long packageId) {
        log.info("Fetching package: {}", packageId);

        AffordablePackage affordablePackage = findPackageById(packageId);

        validatePackageAccess(affordablePackage);


        return affordablePackageMapper.toResponse(affordablePackage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AffordablePackageResponse> getAllActivePackages(Pageable pageable) {
        log.info("Getting all active packages");

        Page<AffordablePackage> packages;

        if (OperatorContext.isSupplierOperator()) {
            Long companyId = OperatorContext.getCurrentCompanyId();
            log.info("Supplier operator requesting active packages - filtering by company ID: {}", companyId);
            packages = affordablePackageRepository.findActiveByCompanyId(companyId, pageable);
        } else {
            log.info("Getting all active packages (system operator or customer)");
            packages = affordablePackageRepository.findAllActive(pageable);
        }

        List<AffordablePackageResponse> responses = packages.getContent().stream()
                .map(affordablePackageMapper::toResponse)
                .toList();

        return PageResponse.of(responses, packages);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AffordablePackageResponse> getAllPackages(Pageable pageable) {
        log.info("Getting all packages for management");

        Page<AffordablePackage> packages;

        if (OperatorContext.isSupplierOperator()) {
            Long companyId = OperatorContext.getCurrentCompanyId();
            log.info("Supplier operator requesting all packages - filtering by company ID: {}", companyId);
            packages = affordablePackageRepository.findByCompanyIdAndDeletedAtIsNull(companyId, pageable);
        } else {
            log.info("System operator requesting all packages - returning all");
            packages = affordablePackageRepository.findAllNotDeleted(pageable);
        }

        List<AffordablePackageResponse> responses = packages.getContent().stream()
                .map(affordablePackageMapper::toResponse)
                .toList();

        return PageResponse.of(responses, packages);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AffordablePackageResponse> getPackagesByCompany(Long companyId, Pageable pageable) {
        log.info("Getting packages for company: {}", companyId);

        findCompanyById(companyId);

        if (OperatorContext.isSupplierOperator()) {
            Long operatorCompanyId = OperatorContext.getCurrentCompanyId();
            if (!operatorCompanyId.equals(companyId)) {
                throw new UnauthorizedOperationException(
                        "You don't have permission to access packages from company: " + companyId);
            }
        }

        Page<AffordablePackage> packages =
                affordablePackageRepository.findByCompanyIdAndDeletedAtIsNull(companyId, pageable);

        List<AffordablePackageResponse> responses = packages.getContent().stream()
                .map(affordablePackageMapper::toResponse)
                .toList();

        return PageResponse.of(responses, packages);
    }

    @Override
    @Transactional
    public AffordablePackageResponse togglePackageStatus(Long packageId, boolean isActive) {
        log.info("Toggling package {} status to: {}", packageId, isActive);

        AffordablePackage affordablePackage = findPackageById(packageId);
        validatePackageAccess(affordablePackage);

        affordablePackage.setIsActive(isActive);
        AffordablePackage updated = affordablePackageRepository.save(affordablePackage);

        log.info("Package {} status changed to: {}", packageId, isActive);
        return affordablePackageMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deletePackage(Long packageId) {
        log.info("Deleting package: {}", packageId);

        AffordablePackage affordablePackage = findPackageById(packageId);
        validatePackageAccess(affordablePackage);

        affordablePackage.softDelete();
        affordablePackageRepository.save(affordablePackage);

        log.info("Package {} deleted", packageId);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private Company findCompanyById(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + companyId));
    }

    private AffordablePackage findPackageById(Long packageId) {
        return affordablePackageRepository.findByIdAndNotDeleted(packageId)
                .orElseThrow(() -> new NotFoundException("Package not found with id: " + packageId));
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
    }

    private void validateProductsAccess(List<PackageProductRequest> products, Long companyId) {
        for (PackageProductRequest productRequest : products) {
            Product product = findProductById(productRequest.getProductId());

            if (product.getCompany() == null || !product.getCompany().getId().equals(companyId)) {
                throw new UnauthorizedOperationException(
                        "Product " + productRequest.getProductId() +
                                " does not belong to your company. You can only add products from your company.");
            }
        }
        log.info("All {} products validated for company: {}", products.size(), companyId);
    }

    private void validatePackageAccess(AffordablePackage affordablePackage) {
        if (OperatorContext.isSupplierOperator()) {
            Long operatorCompanyId = OperatorContext.getCurrentCompanyId();

            if (affordablePackage.getCompany() == null ||
                    !affordablePackage.getCompany().getId().equals(operatorCompanyId)) {
                throw new UnauthorizedOperationException(
                        "You don't have permission to access this package. It doesn't belong to your company.");
            }
        }
    }

    private List<AffordablePackageProduct> createPackageProducts(
            AffordablePackage affordablePackage,
            List<PackageProductRequest> productRequests) {

        List<AffordablePackageProduct> packageProducts = new ArrayList<>();

        for (PackageProductRequest productRequest : productRequests) {
            Product product = findProductById(productRequest.getProductId());

            AffordablePackageProduct packageProduct = new AffordablePackageProduct();
            packageProduct.setAffordablePackage(affordablePackage);
            packageProduct.setProduct(product);
            packageProduct.setQuantity(productRequest.getQuantity());
            packageProducts.add(packageProduct);
        }

        return packageProducts;
    }


    private void validateMaxFrequencyUpdate(AffordablePackage affordablePackage, Integer newMaxFrequency) {
        List<CustomerPackageOrder> activePackageOrders = customerPackageOrderRepository
                .findActivePackagesByPackageId(affordablePackage.getId());

        for (CustomerPackageOrder packageOrder : activePackageOrders) {
            if (packageOrder.getFrequency() > newMaxFrequency) {
                throw new BusinessRuleViolationException(
                        String.format(
                                "Cannot reduce max frequency to %d. " +
                                "There are active package orders with %d deliveries (Order #%s). " +
                                "Please wait until these orders are completed.",
                                newMaxFrequency,
                                packageOrder.getFrequency(),
                                packageOrder.getOrderNumber()
                        )
                );
            }
        }
    }
}