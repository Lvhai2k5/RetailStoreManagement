package ute.fit.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ute.fit.entity.ImportBatchesEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatchesEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ImportBatchesEntity b WHERE b.batchID = :batchId")
    Optional<ImportBatchesEntity> findByIdForUpdate(@Param("batchId") Integer batchId);

    @Query("SELECT b.batchID FROM ImportBatchesEntity b WHERE b.expiryDate IS NOT NULL AND b.expiryDate < :now")
    List<Integer> findExpiredBatchIds(@Param("now") LocalDateTime now);
}
