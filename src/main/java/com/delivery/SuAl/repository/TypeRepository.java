package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Type;
import com.delivery.SuAl.model.ContainerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeRepository extends JpaRepository<Type, Long> {
    Optional<Type> findByContainerType(ContainerType containerType);
}
