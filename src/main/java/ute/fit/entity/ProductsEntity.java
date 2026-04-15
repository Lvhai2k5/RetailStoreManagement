package ute.fit.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; // Import BigDecimal

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ute.fit.model.ProductStatus;

@Entity
@Table(name = "Products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductsEntity {

    @Id
    @Column(name = "ProductID", length = 100)
    private String productID; // Ví dụ: "StingDo", "HaoHaoChuaCay"

    @Column(name = "Name", nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(name = "DefaultSellingPrice", precision = 18, scale = 2)
    private BigDecimal defaultSellingPrice; // Dùng BigDecimal thay cho Double

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 10)
    @Builder.Default
    private ProductStatus status = ProductStatus.BUYING;

    @Column(name = "CreatedDate")
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY) // Nên dùng Lazy để tối ưu hiệu năng
    @JoinColumn(name = "ProductTypeID", nullable = false)
    private ProductTypesMarkupEntity productType;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ImportBatchesEntity> importBatches;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<OrderDetailsEntity> orderDetails;

    @OneToMany(mappedBy = "product",fetch = FetchType.LAZY)
    private List<SupplierProductsEntity> supplierProducts;
    
    @OneToMany(mappedBy = "product")
    private List<ProductPriceHistoryEntity> priceHistories;
}