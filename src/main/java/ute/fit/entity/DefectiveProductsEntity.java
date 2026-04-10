package ute.fit.entity;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DefectiveProducts")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class DefectiveProductsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DefectiveID")
    private Integer defectiveID;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Reason", length = 255)
    private String reason;

    @Column(name = "ImportPrice", precision = 18, scale = 2)
    private BigDecimal importPrice;
    @Column(name = "CreatedDate")
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BatchID", nullable = false)
    private ImportBatchesEntity batch;
}