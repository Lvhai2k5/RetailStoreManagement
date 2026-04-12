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
}