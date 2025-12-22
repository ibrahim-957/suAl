package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.UserContainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserContainerRepository extends JpaRepository<UserContainer,Long> {
    Optional<UserContainer> findByUserIdAndProductId(Long userId,  Long productId);

    List<UserContainer> findByUserId(Long userId);
}
