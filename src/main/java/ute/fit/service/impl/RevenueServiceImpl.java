package ute.fit.service.impl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        LocalDateTime start;
        LocalDateTime end;

        // 1. XÁC ĐỊNH KHOẢNG THỜI GIAN
        if ("custom".equals(range) || (StringUtils.hasText(startDate) && StringUtils.hasText(endDate))) {
            start = LocalDate.parse(startDate).atStartOfDay();
            end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        } else {
            start = calculateStartByRange(range);
            end = calculateEndByRange(range); 
        }

        // 2. TỔNG HỢP DỮ LIỆU TÀI CHÍNH
        Object[] stats = (Object[]) orderRepo.getRevenueStats(start, end).get(0);
        BigDecimal grossRevenue = (stats != null && stats[1] != null) ? (BigDecimal) stats[1] : BigDecimal.ZERO;
        Long totalOrders = (stats != null && stats[0] != null) ? (Long) stats[0] : 0L;

        BigDecimal totalRefund = returnRepo.getTotalRefundAmount(start, end);
        if (totalRefund == null) totalRefund = BigDecimal.ZERO;
        BigDecimal netRevenue = grossRevenue.subtract(totalRefund);

        // 3. XỬ LÝ DANH SÁCH ĐƠN HÀNG CHO EXCEL (Sửa lỗi ClassCastException)
        List<Object[]> rawOrders = orderRepo.getOrdersForExport(start, end);
        List<RevenueReportDTO.OrderExportDetail> orderExportList = rawOrders.stream().map(obj -> {
            Integer id = (Integer) obj[0];
            LocalDateTime date = (LocalDateTime) obj[1];
            BigDecimal total = (BigDecimal) obj[2];
            String status = obj[3].toString(); // Lấy từ Enum OrderStatus
            Long returnCount = (Long) obj[4]; // Đếm số bản ghi trong ReturnsEntity liên kết với Order

            String note = "";
            // Công thức lợi nhuận: Giả định 25% doanh thu
            BigDecimal profit = total.multiply(new BigDecimal("0.25")); 

            // ƯU TIÊN 1: Kiểm tra đơn hủy
            if ("Cancelled".equalsIgnoreCase(status)) {
                note = "Đã hủy";
                profit = BigDecimal.ZERO;
            } 
            // ƯU TIÊN 2: Kiểm tra nếu có dữ liệu trong ReturnsEntity
            else if (returnCount > 0) {
                note = "Trả hàng";
                // Lợi nhuận thực tế = Doanh thu - Tiền hoàn trả (RefundAmount từ ReturnsEntity)
                // Ở đây tạm set bằng 0 cho đơn có trả hàng để an toàn báo cáo
                profit = BigDecimal.ZERO; 
            }
            // ƯU TIÊN 3: Đơn bình thường
            else {
                note = "Hoàn tất";
            }

            return new RevenueReportDTO.OrderExportDetail(id, date.toString(), total, profit, note);
        }).collect(Collectors.toList());

        // 4. TOP 5 SẢN PHẨM (Xử lý an toàn với kiểu dữ liệu số)
        List<Object[]> rawTopProds = detailRepo.getTop5Products(start, end, PageRequest.of(0, 5));
        List<RevenueReportDTO.ProductSalesDTO> mappedTopProducts = rawTopProds.stream()
                .map(obj -> new RevenueReportDTO.ProductSalesDTO(
                    (String) obj[0], 
                    ((Number) obj[1]).longValue(), 
                    new BigDecimal(obj[2].toString())
                )).collect(Collectors.toList());

        // 5. DỮ LIỆU BIỂU ĐỒ (Sửa lỗi ép kiểu Enum sang String)
        Map<String, Long> statusDist = orderRepo.getStatusDistribution(start, end).stream()
                .filter(obj -> obj[0] != null)
                .collect(Collectors.toMap(
                    obj -> obj[0].toString(), 
                    obj -> (Long) obj[1]
                ));

        List<RevenueReportDTO.DailyRevenueDTO> dailyRevenue = orderRepo.getRevenueByDay(start, end).stream()
                .map(obj -> new RevenueReportDTO.DailyRevenueDTO(obj[0].toString(), (BigDecimal) obj[1]))
                .collect(Collectors.toList());

        // 6. BUILD DTO HOÀN CHỈNH
        return RevenueReportDTO.builder()
                .totalRevenue(grossRevenue)
                .totalRefund(totalRefund)
                .netRevenue(netRevenue)
                .totalOrders(totalOrders)
                .statusDistribution(statusDist)
                .dailyRevenue(dailyRevenue)
                .topProducts(mappedTopProducts)
                .orders(orderExportList) // Gán dữ liệu vào list orders
                .build();
    }

    private LocalDateTime calculateStartByRange(String range) {
        LocalDate today = LocalDate.now();
        return switch (range.toLowerCase()) {
            case "week" -> today.with(DayOfWeek.MONDAY).atStartOfDay();
            case "month" -> today.withDayOfMonth(1).atStartOfDay();
            case "year" -> today.withDayOfYear(1).atStartOfDay();
            default -> today.withDayOfMonth(1).atStartOfDay();
        };
    }

    private LocalDateTime calculateEndByRange(String range) {
        LocalDate today = LocalDate.now();
        return switch (range.toLowerCase()) {
            case "week" -> today.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
            case "month" -> today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
            case "year" -> today.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX);
            default -> LocalDateTime.now();
        };
    }
}