package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Company;
import com.delivery.SuAl.model.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByCompanyStatus(CompanyStatus companyStatus);

    Optional<Company> findByName(String name);

    @Query("SELECT c FROM Company c " +
            "WHERE c.companyStatus = 'ACTIVE' " +
            "ORDER BY c.name")
    List<Company> findAllActive();
}
