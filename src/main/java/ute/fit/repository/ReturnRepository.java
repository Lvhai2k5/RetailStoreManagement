package ute.fit.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ute.fit.entity.ReturnsEntity;

@Repository
public interface ReturnRepository extends JpaRepository<ReturnsEntity, Integer> {
    // Tính tổng số tiền đã hoàn trả cho khách
    @Query("SELECT SUM(r.refundAmount) FROM ReturnsEntity r WHERE r.returnDate BETWEEN :start AND :end")
    BigDecimal getTotalRefundAmount(LocalDateTime start, LocalDateTime end);
}
