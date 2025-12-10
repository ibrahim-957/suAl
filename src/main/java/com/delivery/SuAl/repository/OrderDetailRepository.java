package com.delivery.SuAl.repository;

import com.delivery.SuAl.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail,Long> {
    List<OrderDetail> findByOrderId(Long orderId);

    List<OrderDetail> findByProductId(Long productId);

    @Query("SELECT od.product.id, od.product.name, SUM(od.count) as total FROM OrderDetail od " +
            "GROUP BY od.product.id, od.product.name " +
            "ORDER BY total DESC ")
    List<Object[]> findMostSoldProducts();

    @Query("SELECT SUM(od.count) FROM OrderDetail od " +
            "WHERE od.product.id = :productId")
    Long sumQuantityByProduct(@Param("productId") Long productId);
}
