package ute.fit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ute.fit.entity.ImportBatchesEntity;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.repository.query.Param;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatchesEntity, Integer> {
    
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
            (SELECT COALESCE(SUM(dp.quantity), 0) FROM DefectiveProductsEntity dp WHERE dp.batch = b) as defectiveQty
        FROM ImportBatchesEntity b
    """)
    Page<Map<String, Object>> findAllBatchesForUI(Pageable pageable);

    @Query("SELECT SUM(b.quantity * b.importPrice) FROM ImportBatchesEntity b")
    Double sumTotalInventoryValue();

    @Query("SELECT COUNT(b) FROM ImportBatchesEntity b WHERE b.expiryDate < :now")
    long countExpired(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM ImportBatchesEntity b WHERE b.expiryDate BETWEEN :now AND :nextWeek")
    long countExpiringSoon(@Param("now") LocalDateTime now, @Param("nextWeek") LocalDateTime nextWeek);
}