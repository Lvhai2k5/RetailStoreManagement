package ute.fit.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ute.fit.entity.SaleAllocationsEntity;

public interface SaleAllocationRepository
        extends JpaRepository<SaleAllocationsEntity, Integer> {
}