package com.delivery.SuAl.service;

import com.delivery.SuAl.model.request.companyAndcategory.CreateCategoryRequest;
import com.delivery.SuAl.model.request.companyAndcategory.UpdateCategoryRequest;
import com.delivery.SuAl.model.response.companyAndcategory.CategoryResponse;
import com.delivery.SuAl.model.response.wrapper.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest createCategoryRequest);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest updateCategoryRequest);

    void deleteCategory(Long id);

    PageResponse<CategoryResponse> getAllCategories(Pageable pageable);
}
