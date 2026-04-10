package ute.fit.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import ute.fit.dto.RevenueReportDTO;
import java.io.IOException;
import java.time.LocalDate;

public class ExportUtils {

    public static void exportRevenueToExcel(HttpServletResponse response, RevenueReportDTO report, String startStr, String endStr) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo doanh thu");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0\"đ\""));

            // 1. Phần Tổng Quan
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("BÁO CÁO KẾT QUẢ KINH DOANH");
            
            Row periodRow = sheet.createRow(1);
            periodRow.createCell(0).setCellValue("Khoảng thời gian: " + (startStr != null ? startStr : "...") + " đến " + (endStr != null ? endStr : "..."));

            int rowIdx = 3;
            createRow(sheet, rowIdx++, "Tổng doanh thu bán ra (Paid):", report.getTotalRevenue().doubleValue(), currencyStyle);
            createRow(sheet, rowIdx++, "Tổng tiền trả hàng:", report.getTotalRefund().doubleValue(), currencyStyle);
            createRow(sheet, rowIdx++, "Lợi nhuận thực tế:", report.getNetRevenue().doubleValue(), currencyStyle);

            // 2. Phần Danh Sách Đơn Hàng
            rowIdx += 2;
            sheet.createRow(rowIdx++).createCell(0).setCellValue("DANH SÁCH ĐƠN HÀNG CHI TIẾT");

            Row headerRow = sheet.createRow(rowIdx++);
            String[] columns = {"Mã đơn", "Ngày mua", "Tổng tiền đơn", "Lợi nhuận", "Ghi chú"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // DUYỆT DANH SÁCH ĐƠN HÀNG
            if (report.getOrders() != null) {
                for (var o : report.getOrders()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(o.getOrderId());
                    row.createCell(1).setCellValue(o.getDate());
                    
                    Cell totalCell = row.createCell(2);
                    totalCell.setCellValue(o.getTotalAmount().doubleValue());
                    totalCell.setCellStyle(currencyStyle);

                    Cell profitCell = row.createCell(3);
                    profitCell.setCellValue(o.getProfit().doubleValue());
                    profitCell.setCellStyle(currencyStyle);

                    row.createCell(4).setCellValue(o.getNote() != null ? o.getNote() : "");
                }
            }

            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

            String fileName = "Bao_cao_kinh_doanh_" + LocalDate.now() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            workbook.write(response.getOutputStream());
        }
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void createRow(Sheet sheet, int rowIdx, String label, Object value, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        if (value instanceof Number num) valueCell.setCellValue(num.doubleValue());
        else valueCell.setCellValue(value.toString());
        if (style != null) valueCell.setCellStyle(style);
    }
}