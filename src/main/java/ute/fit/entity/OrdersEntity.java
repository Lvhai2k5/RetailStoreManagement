package ute.fit.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;
// Giả sử bạn đã tạo các Enum này trong package model
import ute.fit.model.OrderStatus; 


@Entity
@Table(name = "Orders")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class OrdersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderID;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.Pending; // Pending, Paid, Cancelled

    @Column(name = "PaymentMethod", length = 20)
    private String paymentMethod; // Cash, Transfer

    @Column(name = "TotalAmount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "CreatedDate")
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "PaidDate")
    private LocalDateTime paidDate;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private List<OrderDetailsEntity> orderDetails;

    // Quan hệ với bảng trả hàng (nếu cần quản lý từ phía Order)
    @OneToMany(mappedBy = "order")
    private List<ReturnsEntity> returns;
}