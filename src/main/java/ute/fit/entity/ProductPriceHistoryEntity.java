package ute.fit.entity;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ProductPriceHistory")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ProductPriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PriceID")
    private Integer priceID;

    @Column(name = "SellingPrice", precision = 18, scale = 2, nullable = false)
    private BigDecimal sellingPrice; // Giá bán áp dụng ra thị trường

    @Column(name = "ChangeReason", nullable = false, length = 50)
    private String changeReason; // MANUAL hoặc NEW_BATCH

    @Column(name = "EffectiveDate")
    @Builder.Default
    private LocalDateTime effectiveDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private ProductsEntity product;
}