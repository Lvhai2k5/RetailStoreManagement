package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ute.fit.entity.InventoryTransactionsEntity;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionsEntity, Integer> {
}
