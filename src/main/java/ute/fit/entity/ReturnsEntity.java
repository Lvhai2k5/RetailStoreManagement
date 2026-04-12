package ute.fit.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Returns")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ReturnsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReturnID")
    private Integer returnID;

    @Builder.Default
    @Column(name = "ReturnDate", nullable = false)
    private LocalDateTime returnDate = LocalDateTime.now();

    @Column(name = "RefundAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private OrdersEntity order;

    @Builder.Default
    @OneToMany(mappedBy = "returnEntry", fetch = FetchType.LAZY)
    private List<ReturnDetailsEntity> returnDetails = new ArrayList<>();
}