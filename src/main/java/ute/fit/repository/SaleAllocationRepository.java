package ute.fit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ute.fit.entity.SaleAllocationsEntity;

@Repository
public interface SaleAllocationRepository extends JpaRepository<SaleAllocationsEntity, Integer> {
    List<SaleAllocationsEntity> findByOrderDetailOrderDetailIDOrderByAllocationID(Integer orderDetailId);
}
