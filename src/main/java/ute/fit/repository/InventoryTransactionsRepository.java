package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ute.fit.entity.InventoryTransactionsEntity;

@Repository
public interface InventoryTransactionsRepository extends JpaRepository<InventoryTransactionsEntity, Integer> {

    @Query("""
            SELECT COALESCE(SUM(it.quantityChange), 0)
            FROM InventoryTransactionsEntity it
            WHERE it.batch.batchID = :batchId
              AND it.isSellable = true
            """)
    Integer getSellableStockByBatchId(@Param("batchId") Integer batchId);
}
