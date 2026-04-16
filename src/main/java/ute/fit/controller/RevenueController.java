package ute.fit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import ute.fit.dto.RevenueReportDTO;
import ute.fit.service.IRevenueService;

@Controller
@RequestMapping("/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final IRevenueService revenueService;

    @GetMapping
    public String viewRevenueReport(
            @RequestParam(value = "range", defaultValue = "month") String range,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            Model model) {

        // Gọi Service lấy dữ liệu tổng hợp
        RevenueReportDTO report = revenueService.getFullReport(range, startDate, endDate);

        // Đưa dữ liệu ra View
        model.addAttribute("report", report);
        model.addAttribute("currentRange", range);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "revenue-report"; // Trỏ tới file HTML của bạn
    }

    // Endpoint chuẩn bị cho chức năng xuất Excel
    @GetMapping("/export/excel")
    public void exportToExcel(
            @RequestParam(value = "range", defaultValue = "month") String range,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            HttpServletResponse response) { // <--- QUAN TRỌNG: Phải thêm tham số này

        try {
            // 1. Khai báo kiểu nội dung là file Excel
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            
            // 2. Ra lệnh cho trình duyệt tải file xuống với tên cụ thể
            response.setHeader("Content-Disposition", "attachment; filename=Bao_Cao_Doanh_Thu_Tam_Thoi.xlsx");

            // 3. Logic gọi sang Service xuất Excel sẽ nằm ở đây
            // (Hiện tại đang để trống nên khi bạn bấm tải, nó sẽ tải về một file Excel lỗi/trống)
            // excelExportService.exportRevenueReport(report, response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}