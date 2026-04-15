package ute.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "ProductTypesMarkup")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ProductTypesMarkupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductTypeID") 
    private Integer productTypeID;

    @Column(name = "Name", nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(name = "MarkupPercent", precision = 5, scale = 2, nullable = false)
    private BigDecimal markupPercent;

    @OneToMany(mappedBy = "productType", fetch = FetchType.LAZY)
    // Nên cân nhắc dùng @JsonIgnore nếu bạn viết REST API để tránh vòng lặp vô tận
    private List<ProductsEntity> products;
}