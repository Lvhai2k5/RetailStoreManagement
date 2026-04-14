package ute.fit.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ute.fit.entity.ImportBatchesEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatchesEntity, Integer> {

    /**
     * Lock batch để tránh race condition (xuất kho, bán hàng)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ImportBatchesEntity b WHERE b.batchID = :batchId")
    Optional<ImportBatchesEntity> findByIdForUpdate(@Param("batchId") Integer batchId);

    /**
     * Lấy danh sách batch đã hết hạn
     */
    @Query("""
        SELECT b.batchID 
        FROM ImportBatchesEntity b 
        WHERE b.expiryDate IS NOT NULL 
          AND b.expiryDate < :now
    """)
    List<Integer> findExpiredBatchIds(@Param("now") LocalDateTime now);

    /**
     * Lấy danh sách batch cho UI (dashboard/table)
     */
    @Query("""
        SELECT 
            b.batchID as batchID,
            b.product.productID as productID,
            b.product.name as productName,
            b.supplier.name as supplierName, 
            b.quantity as importQty,
            b.importPrice as importPrice,
            b.sellingPrice as sellingPrice,
            b.importDate as importDate,
            b.expiryDate as expiryDate,
            (SELECT COALESCE(SUM(dp.quantity), 0) 
             FROM DefectiveProductsEntity dp 
             WHERE dp.batch = b) as defectiveQty
        FROM ImportBatchesEntity b
    """)
    Page<Map<String, Object>> findAllBatchesForUI(Pageable pageable);

    /**
     * Tổng giá trị tồn kho (inventory value)
     */
    @Query("""
        SELECT COALESCE(SUM(b.quantity * b.importPrice), 0)
        FROM ImportBatchesEntity b
    """)
    Double sumTotalInventoryValue();

    /**
     * Đếm batch hết hạn
     */
    @Query("""
        SELECT COUNT(b) 
        FROM ImportBatchesEntity b 
        WHERE b.expiryDate < :now
    """)
    long countExpired(@Param("now") LocalDateTime now);

    /**
     * Đếm batch sắp hết hạn
     */
    @Query("""
        SELECT COUNT(b) 
        FROM ImportBatchesEntity b 
        WHERE b.expiryDate BETWEEN :now AND :nextWeek
    """)
    long countExpiringSoon(@Param("now") LocalDateTime now,
                           @Param("nextWeek") LocalDateTime nextWeek);

    /**
     * Lấy batchNumber lớn nhất theo Product (để auto tăng)
     */
    @Query("""
        SELECT COALESCE(MAX(b.batchNumber), 0) 
        FROM ImportBatchesEntity b 
        WHERE b.product.productID = :productId
    """)
    Integer findMaxBatchNumberByProductId(@Param("productId") String productId);

    /**
     * Lấy danh sách batch theo Product (FIFO)
     */
    @Query("""
        SELECT b 
        FROM ImportBatchesEntity b
        WHERE b.product.productID = :productID
        ORDER BY b.batchNumber
    """)
    List<ImportBatchesEntity> findByProductID(@Param("productID") String productID);
}