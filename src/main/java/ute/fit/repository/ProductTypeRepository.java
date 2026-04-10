package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ute.fit.entity.ProductTypesMarkupEntity;

public interface ProductTypeRepository 
        extends JpaRepository<ProductTypesMarkupEntity, Integer> {
}