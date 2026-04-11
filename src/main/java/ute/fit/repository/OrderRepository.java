package ute.fit.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ute.fit.entity.OrdersEntity;

@Repository
public interface OrderRepository extends JpaRepository<OrdersEntity, Integer> {
    // Thống kê doanh thu và số đơn (Chỉ tính đơn đã Paid)
    @Query("SELECT COUNT(o), SUM(o.totalAmount) FROM OrdersEntity o " +
           "WHERE o.status = 'Paid' AND o.createdDate BETWEEN :start AND :end")
    List<Object[]> getRevenueStats(LocalDateTime start, LocalDateTime end);

    // Thống kê trạng thái để vẽ biểu đồ tròn (Pending, Paid, Cancelled)
    @Query("SELECT o.status, COUNT(o) FROM OrdersEntity o " +
           "WHERE o.createdDate BETWEEN :start AND :end GROUP BY o.status")
    List<Object[]> getStatusDistribution(LocalDateTime start, LocalDateTime end);

    // Lấy doanh thu theo ngày để vẽ biểu đồ đường
    @Query("SELECT CAST(o.createdDate AS date), SUM(o.totalAmount) FROM OrdersEntity o " +
           "WHERE o.status = ute.fit.model.OrderStatus.Paid AND o.createdDate BETWEEN :start AND :end " +
           "GROUP BY CAST(o.createdDate AS date) ORDER BY CAST(o.createdDate AS date)")
    List<Object[]> getRevenueByDay(LocalDateTime start, LocalDateTime end);

    List<OrdersEntity> findByStatus(ute.fit.model.OrderStatus status);
}