package ute.fit.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ReturnDetails")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class ReturnDetailsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReturnDetailID")
    private Integer returnDetailID;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "RefundUnitPrice", precision = 18, scale = 2, nullable = false)
    private BigDecimal refundUnitPrice;

    @Column(name = "Reason", length = 255, columnDefinition = "VARCHAR(255)")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReturnID", nullable = false)
    private ReturnsEntity returnEntry; // Đổi tên cho khớp với mappedBy ở ReturnsEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderDetailID", nullable = false)
    private OrderDetailsEntity orderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BatchID", nullable = false) // Bổ sung để khớp với DDL
    private ImportBatchesEntity batch;
}