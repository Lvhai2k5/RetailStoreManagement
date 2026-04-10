package ute.fit.entity;

import java.time.LocalDateTime;
import java.math.BigDecimal; // Quan trọng cho tài chính
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ImportBatches")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ImportBatchesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BatchID")
    private Integer batchID;

    @Column(name = "BatchNumber", nullable = false)
    private Integer batchNumber; // Số lô của sản phẩm đó

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "ImportPrice", precision = 18, scale = 2, nullable = false)
    private BigDecimal importPrice;

    @Column(name = "SellingPrice", precision = 18, scale = 2, nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "ImportDate")
    @Builder.Default
    private LocalDateTime importDate = LocalDateTime.now();

    @Column(name = "ExpiryDate")
    private LocalDateTime expiryDate; // Có thể null nếu hàng không có hạn dùng

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private ProductsEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SupplierID", nullable = false)
    private SuppliersEntity supplier;
    
    // Nếu bạn muốn quản lý log biến động kho từ phía này:
    @OneToMany(mappedBy = "batch", fetch = FetchType.LAZY)
    private java.util.List<InventoryTransactionsEntity> inventoryTransactions;
}