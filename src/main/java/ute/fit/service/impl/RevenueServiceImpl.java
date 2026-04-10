package ute.fit.service.impl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ute.fit.dto.RevenueReportDTO;
import ute.fit.repository.OrderDetailRepository;
import ute.fit.repository.OrderRepository;
import ute.fit.repository.ReturnRepository;
import ute.fit.service.IRevenueService;

@Service
@RequiredArgsConstructor
public class RevenueServiceImpl implements IRevenueService {
    private final OrderRepository orderRepo;
    private final ReturnRepository returnRepo;
    private final OrderDetailRepository detailRepo;

    @Override
    public RevenueReportDTO getFullReport(String range, String startDate, String endDate) {
        LocalDateTime start = calculateStart(range, startDate);
        LocalDateTime end = (range.equals("custom") && endDate != null) 
                            ? LocalDate.parse(endDate).atTime(LocalTime.MAX) 
                            : LocalDateTime.now();

        // 1. Thống kê doanh thu gốc và số đơn từ OrdersEntity
        Object[] stats = (Object[]) orderRepo.getRevenueStats(start, end).get(0);
        BigDecimal grossRevenue = (stats != null && stats[1] != null) ? (BigDecimal) stats[1] : BigDecimal.ZERO;
        Long totalOrders = (stats != null && stats[0] != null) ? (Long) stats[0] : 0L;

        // 2. Trừ đi tiền hoàn trả từ ReturnsEntity
        BigDecimal totalRefund = returnRepo.getTotalRefundAmount(start, end);
        if (totalRefund == null) totalRefund = BigDecimal.ZERO;
        BigDecimal netRevenue = grossRevenue.subtract(totalRefund);

        // 3. Thống kê trạng thái đơn hàng (Dùng cho biểu đồ tròn)
        Map<String, Long> statusDist = orderRepo.getStatusDistribution(start, end).stream()
                .collect(Collectors.toMap(obj -> obj[0].toString(), obj -> (Long) obj[1]));

        // 4. Lấy doanh thu theo ngày (Dùng cho biểu đồ đường)
        List<RevenueReportDTO.DailyRevenueDTO> dailyRevenue = orderRepo.getRevenueByDay(start, end).stream()
                .map(obj -> new RevenueReportDTO.DailyRevenueDTO(obj[0].toString(), (BigDecimal) obj[1]))
                .collect(Collectors.toList());
        
        // 5. SỬA LỖI: Mapping Top 5 sản phẩm từ List<Object[]> sang List<ProductSalesDTO>
        List<Object[]> rawTopProds = detailRepo.getTop5Products(start, end, PageRequest.of(0, 5));
        List<RevenueReportDTO.ProductSalesDTO> mappedTopProducts = rawTopProds.stream()
                .map(obj -> new RevenueReportDTO.ProductSalesDTO(
                    (String) obj[0],      // Tên sản phẩm
                    (Long) obj[1],        // Số lượng bán
                    (BigDecimal) obj[2]   // Doanh thu sản phẩm
                ))
                .collect(Collectors.toList());

        // 6. Đóng gói vào DTO
        return RevenueReportDTO.builder()
                .totalRevenue(grossRevenue)
                .totalRefund(totalRefund)
                .netRevenue(netRevenue)
                .totalOrders(totalOrders)
                .statusDistribution(statusDist)
                .dailyRevenue(dailyRevenue)
                .topProducts(mappedTopProducts) // Gán danh sách đã được chuyển đổi kiểu dữ liệu
                .build();
    }
    
    private LocalDateTime calculateStart(String range, String date) {
        return switch (range) {
            case "week" -> LocalDateTime.now().with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
            case "month" -> LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
            case "year" -> LocalDateTime.now().withDayOfYear(1).toLocalDate().atStartOfDay();
            case "custom" -> (date != null) ? LocalDate.parse(date).atStartOfDay() : LocalDateTime.now().minusDays(30);
            default -> LocalDateTime.now().minusDays(30);
        };
    }
}