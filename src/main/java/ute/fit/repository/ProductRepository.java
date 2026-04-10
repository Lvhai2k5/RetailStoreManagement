package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ute.fit.entity.ProductsEntity;

public interface ProductRepository extends JpaRepository<ProductsEntity, String> {}