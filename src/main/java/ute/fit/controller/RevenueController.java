package ute.fit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ute.fit.dto.RevenueReportDTO;
import ute.fit.service.IRevenueService;

import java.time.LocalDate;

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
            @RequestParam(value = "endDate", required = false) String endDate) {
        // Logic gọi sang ExportUtils sẽ triển khai sau
    }
}