package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ute.fit.entity.SuppliersEntity;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<SuppliersEntity, Integer> {

    @Query("""
            SELECT s FROM SuppliersEntity s
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR s.phone LIKE CONCAT('%', :keyword, '%'))
            ORDER BY s.supplierID DESC
            """)
    List<SuppliersEntity> search(@Param("keyword") String keyword);
}
