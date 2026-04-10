package ute.fit.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import ute.fit.model.TransactionType;

@Entity
@Table(name = "InventoryTransactions")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class InventoryTransactionsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionID")
    private Integer transactionID;

    @Column(name = "QuantityChange", nullable = false)
    private Integer quantityChange; // + nhập/trả hàng, - bán/hàng lỗi

    @Enumerated(EnumType.STRING)
    @Column(name = "TransactionType", nullable = false, length = 20)
    private TransactionType transactionType;
    
    @Column(name = "IsSellable", nullable = false)
    @Builder.Default
    private Boolean isSellable = true;

    @Column(name = "ReferenceID")
    private Integer referenceID; // ID tham chiếu (OrderID, ReturnID...)

    @Column(name = "TransactionDate")
    @Builder.Default
    private LocalDateTime transactionDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BatchID", nullable = false)
    private ImportBatchesEntity batch;
}