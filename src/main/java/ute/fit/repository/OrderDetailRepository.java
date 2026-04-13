package ute.fit.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ute.fit.entity.OrderDetailsEntity;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetailsEntity, Integer> {
    // Top 5 sản phẩm bán chạy nhất
    @Query("SELECT od.product.name, SUM(od.quantity), SUM(od.quantity * od.unitPrice) " +
           "FROM OrderDetailsEntity od WHERE od.order.status = ute.fit.model.OrderStatus.Paid " +
           "AND od.order.createdDate BETWEEN :start AND :end " +
           "GROUP BY od.product.name ORDER BY SUM(od.quantity) DESC")
    List<Object[]> getTop5Products(LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<OrderDetailsEntity> findByOrder_OrderID(Integer orderId);
}
