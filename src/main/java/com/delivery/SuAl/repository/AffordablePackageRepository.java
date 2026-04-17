package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.AffordablePackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AffordablePackageRepository extends JpaRepository<AffordablePackage, Long> {
    @Query("SELECT ap FROM AffordablePackage ap " +
            "WHERE ap.deletedAt IS NULL AND ap.id = :id")
    Optional<AffordablePackage> findByIdAndNotDeleted(@Param("id") Long id);

    @Query("SELECT ap FROM AffordablePackage ap " +
            "WHERE ap.deletedAt IS NULL AND ap.isActive = true")
    Page<AffordablePackage> findAllActive(Pageable pageable);

    @Query("SELECT ap FROM AffordablePackage ap " +
            "WHERE ap.deletedAt IS NULL AND ap.company.id = :companyId")
    Page<AffordablePackage> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT ap FROM AffordablePackage ap " +
            "WHERE ap.deletedAt IS NULL AND ap.company.id = :companyId AND ap.isActive = true")
    Page<AffordablePackage> findByCompanyIdAndActive(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT ap FROM AffordablePackage ap " +
            "WHERE ap.deletedAt IS NULL")
    Page<AffordablePackage> findAllNotDeleted(Pageable pageable);

    Page<AffordablePackage> findByCompanyIdAndDeletedAtIsNull(Long companyId, Pageable pageable);

    Page<AffordablePackage> findActiveByCompanyId(Long companyId, Pageable pageable);
}
