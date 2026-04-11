package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ute.fit.entity.DefectiveProductsEntity;

@Repository
public interface DefectiveProductsRepository extends JpaRepository<DefectiveProductsEntity, Integer> {
}
