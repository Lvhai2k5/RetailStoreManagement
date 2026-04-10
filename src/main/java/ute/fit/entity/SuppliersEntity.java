package ute.fit.entity;

import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import ute.fit.model.SupplierStatus;

@Entity
@Table(name = "Suppliers")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class SuppliersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SupplierID")
    private Integer supplierID;

    @Column(name = "Name", nullable = false, length = 255)
    private String name;

    @Column(name = "Phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "Address", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private SupplierStatus status = SupplierStatus.COLLAB;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<ImportBatchesEntity> importBatches;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<SupplierProductsEntity> supplierProducts;
}