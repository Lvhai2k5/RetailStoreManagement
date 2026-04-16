package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ute.fit.entity.InventoryTransactionsEntity;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionsEntity, Integer> {

    @Query("SELECT SUM(t.quantityChange) FROM InventoryTransactionsEntity t WHERE t.batch.batchID = :batchId")
    Integer sumQuantityChangeByBatchId(@Param("batchId") Integer batchId);

    @Query("SELECT t FROM InventoryTransactionsEntity t WHERE t.batch.batchID = :batchId ORDER BY t.transactionDate DESC")
    List<InventoryTransactionsEntity> findByBatchId(@Param("batchId") Integer batchId);

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN t.transactionType IN ('IMPORT', 'CANCEL_RESERVE') THEN t.quantityChange 
                WHEN t.transactionType IN ('SALE', 'RESERVE') THEN -t.quantityChange 
                ELSE 0 
            END
        ), 0)
        FROM InventoryTransactionsEntity t
        JOIN t.batch b
        WHERE b.product.productID = :productID
    """)
    Integer getAvailableStock(@Param("productID") int productID);

    @Query("""
        SELECT COALESCE(SUM(i.quantityChange), 0)
        FROM InventoryTransactionsEntity i
        WHERE i.batch.batchID = :batchID
          AND i.isSellable = true
    """)
    int sumStockByBatch(@Param("batchID") int batchID);
}