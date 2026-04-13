package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ute.fit.entity.ProductsEntity;

public interface ProductRepository extends JpaRepository<ProductsEntity, String> {

    @Query(value = "SELECT dbo.FN_GetMaxAvailableStock(:productId)", nativeQuery = true)
    Integer getMaxStock(@Param("productId") String productId);
}