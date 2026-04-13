package ute.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ute.fit.entity.ReturnDetailsEntity;

@Repository
public interface ReturnDetailRepository extends JpaRepository<ReturnDetailsEntity, Integer> {
    @Query("SELECT COALESCE(SUM(rd.quantity), 0) FROM ReturnDetailsEntity rd " +
           "WHERE rd.orderDetail.orderDetailID = :orderDetailId")
    Integer getReturnedQuantityByOrderDetailId(@Param("orderDetailId") Integer orderDetailId);
}
