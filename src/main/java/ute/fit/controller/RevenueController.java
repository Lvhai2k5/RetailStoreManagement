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
import ute.fit.util.ExcelExportUtils;

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

    @GetMapping("/export/excel")
    public void exportToExcel(
            @RequestParam(value = "range", defaultValue = "month") String range,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            HttpServletResponse response) {

        try {
            // 1. Lấy dữ liệu từ Service
            RevenueReportDTO report = revenueService.getFullReport(range, startDate, endDate);

            // 2. Tạo tên file động: BaoCaoDoanhthu_yyyy-MM-dd_yyyy-MM-dd.xlsx
            String fileName = "BaoCaoDoanhthu_" + 
                             (startDate != null ? startDate : "Start") + "_" + 
                             (endDate != null ? endDate : "End") + ".xlsx";

            // 3. Thiết lập thông tin phản hồi (Headers)
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            // 4. Gọi Utils để ghi dữ liệu trực tiếp vào Response OutputStream
            ExcelExportUtils.exportRevenueReport(report, startDate, endDate, response);
            
            // 5. Quan trọng: Flush để đảm bảo dữ liệu được đẩy xuống trình duyệt
            response.flushBuffer();

        } catch (Exception e) {
            e.printStackTrace();
            // Có thể bổ sung log lỗi ở đây
        }
    }
}