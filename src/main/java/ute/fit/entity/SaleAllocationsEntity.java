package ute.fit.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SaleAllocations")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class SaleAllocationsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AllocationID")
    private Integer allocationID;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity; // Số lượng lấy từ lô BatchID cho dòng đơn hàng này

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderDetailID", nullable = false)
    private OrderDetailsEntity orderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BatchID", nullable = false)
    private ImportBatchesEntity batch;
}