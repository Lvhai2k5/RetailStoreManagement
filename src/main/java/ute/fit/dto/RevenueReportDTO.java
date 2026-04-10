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
    private BigDecimal totalRevenue;    
    private BigDecimal totalRefund;     
    private BigDecimal netRevenue;      
    private Long totalOrders;           

    private Map<String, Long> statusDistribution; 
    private List<DailyRevenueDTO> dailyRevenue;    
    private List<ProductSalesDTO> topProducts; 
    
    // Thêm danh sách đơn hàng để xuất Excel
    private List<OrderExportDetail> orders; 

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
        private Long quantity;      
        private BigDecimal revenue; 
    }

    @Data
    @AllArgsConstructor
    public static class OrderExportDetail {
        private Integer orderId;
        private String date;
        private BigDecimal totalAmount;
        private BigDecimal profit;
        private String note; // "Đã hủy", "Trả hàng", hoặc để trống
    }
}