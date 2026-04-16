package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ute.fit.entity.ProductPriceHistoryEntity;

@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistoryEntity, Integer> {
}