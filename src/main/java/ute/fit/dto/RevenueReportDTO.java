package ute.fit.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDTO {
    // KPI chính
    private BigDecimal totalRevenue;    // Tính từ OrdersEntity.totalAmount
    private BigDecimal totalRefund;     // Tính từ ReturnsEntity.refundAmount
    private BigDecimal netRevenue;      // Doanh thu thuần (Revenue - Refund)
    private Long totalOrders;           // Tổng số đơn hàng thành công

    // Dữ liệu biểu đồ
    private Map<String, Long> statusDistribution; // Biểu đồ tròn: Pending, Paid, Cancelled
    private List<DailyRevenueDTO> dailyRevenue;    // Biểu đồ đường: Doanh thu theo ngày

    // Top sản phẩm
    private List<ProductSalesDTO> topProducts; // Top 5 sản phẩm

    @Data
    @AllArgsConstructor
    public static class DailyRevenueDTO {
        private String date;
        private BigDecimal value;
    }

    @Data
    @AllArgsConstructor
    public static class ProductSalesDTO {
        private String productName;
        private Long quantity;      // Tổng OrderDetailsEntity.quantity
        private BigDecimal revenue; // Tổng quantity * unitPrice
    }
}