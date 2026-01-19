package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.Payment;
import com.delivery.SuAl.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReferenceId(String referenceId);

    Optional<Payment> findByOrderId(Long orderId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Payment p WHERE p.order.id = :orderId AND p.paymentStatus = 'SUCCESS'")
    boolean hasSuccessfulPayment(@Param("orderId") Long orderId);

    Optional<Payment> findByOrder_IdAndPaymentStatus(Long orderId, PaymentStatus status);


    Optional<Payment> findByOrderIdAndPaymentStatus(Long orderId, PaymentStatus paymentStatus);
}
