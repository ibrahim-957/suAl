package com.delivery.SuAl.service;

import com.delivery.SuAl.entity.Category;
import com.delivery.SuAl.mapper.CategoryMapper;
import com.delivery.SuAl.model.request.companyAndcategory.CreateCategoryRequest;
import com.delivery.SuAl.model.request.companyAndcategory.UpdateCategoryRequest;
import com.delivery.SuAl.model.response.companyAndcategory.CategoryResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import com.delivery.SuAl.repository.CategoryRepository;
import com.delivery.SuAl.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest createCategoryRequest) {
        log.info("Creating new category with type: {}", createCategoryRequest.getCategoryType());

        categoryRepository.findByCategoryType(createCategoryRequest.getCategoryType())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Category already exists with type: "
                            + createCategoryRequest.getCategoryType());
                });

        Category category = categoryMapper.toEntity(createCategoryRequest);
        Category savedCategory = categoryRepository.save(category);

        log.info("Created category with type: {}", category.getCategoryType());

        CategoryResponse categoryResponse = categoryMapper.toResponse(savedCategory);
        categoryResponse.setProductCount(0L);

        log.info("Category created successfully with ID: {}", savedCategory.getId());
        return categoryResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.info("Fetching category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + id));

        CategoryResponse response = categoryMapper.toResponse(category);
        response.setProductCount(productRepository.countByCategoryId(id));

        log.info("Category found: {}", category.getCategoryType());
        return response;
    }


    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest updateCategoryRequest) {
        log.info("Updating category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + id));

        categoryMapper.updateEntityFromRequest(updateCategoryRequest, category);
        Category  updatedCategory = categoryRepository.save(category);

        CategoryResponse categoryResponse = categoryMapper.toResponse(updatedCategory);
        categoryResponse.setProductCount(productRepository.countByCategoryId(id));

        log.info("Category updated successfully with ID: {}", id);
        return categoryResponse;
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
        log.info("Category deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAllCategories(Pageable pageable) {
        log.info("Fetching all categories");

        Page<Category> categoryPage = categoryRepository.findAll(pageable);

        List<CategoryResponse> categoryResponseList = categoryPage.getContent().stream()
                .map(category -> {
                    CategoryResponse categoryResponse = categoryMapper.toResponse(category);
                    categoryResponse.setProductCount(productRepository.countByCategoryId(category.getId()));
                    return categoryResponse;
                })
                .collect(Collectors.toList());
        return PageResponse.of(categoryResponseList, categoryPage);
    }
}
