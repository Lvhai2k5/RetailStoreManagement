package ute.fit.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ute.fit.entity.OrderDetailsEntity;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetailsEntity, Integer> {
    
    @Query(value = "SELECT p.name, " +
           "SUM(od.quantity) - ISNULL((SELECT SUM(rd.quantity) FROM return_details rd WHERE rd.order_detailid = od.order_detailid), 0) as final_qty, " +
           "SUM(od.quantity * od.unit_price) - ISNULL((SELECT SUM(rd.quantity * rd.refund_unit_price) FROM return_details rd WHERE rd.order_detailid = od.order_detailid), 0) as final_revenue " +
           "FROM order_details od " +
           "JOIN products p ON od.productid = p.productid " +
           "JOIN orders o ON od.orderid = o.orderid " +
           "WHERE o.status = 'Paid' AND o.created_date BETWEEN :start AND :end " +
           "GROUP BY p.name, od.order_detailid " +
           "ORDER BY final_qty DESC", nativeQuery = true)
    List<Object[]> getTop5Products(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);
}