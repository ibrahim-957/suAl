package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Category;
import com.delivery.SuAl.model.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCategoryType(CategoryType categoryType);

}
