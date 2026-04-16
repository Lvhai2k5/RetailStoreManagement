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
    private List<DailyRevenueDTO> dailyRevenue;   // Biểu đồ đường: Doanh thu theo ngày

    // Top sản phẩm
    private List<ProductSalesDTO> topProducts;    // Top 5 sản phẩm

    @Data
    @NoArgsConstructor // Thêm constructor không tham số để an toàn khi parse JSON (nếu cần)
    @AllArgsConstructor
    public static class DailyRevenueDTO {
        private String date;
        private BigDecimal value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSalesDTO {
        private String productName;
        
        // Cập nhật tên biến để đồng bộ với phương thức getSoldQuantity() trong ExcelExportUtils
        private Long soldQuantity;      // Tổng OrderDetailsEntity.quantity
        
        // Cập nhật tên biến để đồng bộ với phương thức getProductRevenue() trong ExcelExportUtils
        private BigDecimal productRevenue; // Tổng quantity * unitPrice
    }
}