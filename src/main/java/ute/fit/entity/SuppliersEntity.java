package ute.fit.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Column(name = "Name", nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(name = "Phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "Address", columnDefinition = "TEXT", nullable = false)
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