package ute.fit.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SupplierProducts")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class SupplierProductsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SupplierProductID")
    private Integer supplierProductID;

    @Column(name = "SupplierProductCode", length = 100)
    private String supplierProductCode;

    @ManyToOne(fetch = FetchType.LAZY) // Tối ưu hiệu năng, tránh load NCC khi không cần
    @JoinColumn(name = "SupplierID", nullable = false) // Khóa ngoại không được null theo DDL
    private SuppliersEntity supplier;

    @ManyToOne(fetch = FetchType.LAZY) // Tối ưu hiệu năng, tránh load Sản phẩm khi không cần
    @JoinColumn(name = "ProductID", nullable = false) // Khóa ngoại không được null theo DDL
    private ProductsEntity product;
}