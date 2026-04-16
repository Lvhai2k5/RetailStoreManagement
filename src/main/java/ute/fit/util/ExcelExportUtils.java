package ute.fit.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ute.fit.dto.RevenueReportDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class ExcelExportUtils {

    public static void exportRevenueReport(RevenueReportDTO report, String startDate, String endDate, HttpServletResponse response) throws IOException {
        // Sử dụng Try-with-resources để đảm bảo đóng Workbook và Stream
        try (Workbook workbook = new XSSFWorkbook(); 
             OutputStream out = response.getOutputStream()) {
             
            Sheet sheet = workbook.createSheet("Bao Cao");

            // --- ĐỊNH DẠNG (STYLE) ---
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0\"đ\""));

            // --- 1. TIÊU ĐỀ & THỜI GIAN ---
            Row rowTitle = sheet.createRow(0);
            rowTitle.createCell(0).setCellValue("BÁO CÁO KẾT QUẢ KINH DOANH");
            
            Row rowTime = sheet.createRow(1);
            rowTime.createCell(0).setCellValue("Khoảng thời gian: " + (startDate != null ? startDate : "...") + " đến " + (endDate != null ? endDate : "..."));

            // --- 2. THÔNG SỐ TỔNG QUÁT ---
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Tổng doanh thu (Paid):");
            Cell cell3 = row3.createCell(1);
            cell3.setCellValue(report.getTotalRevenue() != null ? report.getTotalRevenue().doubleValue() : 0);
            cell3.setCellStyle(currencyStyle);

            Row row4 = sheet.createRow(4);
            row4.createCell(0).setCellValue("Tổng tiền trả hàng:");
            Cell cell4 = row4.createCell(1);
            cell4.setCellValue(report.getTotalRefund() != null ? report.getTotalRefund().doubleValue() : 0);
            cell4.setCellStyle(currencyStyle);

            Row row5 = sheet.createRow(5);
            row5.createCell(0).setCellValue("Lợi nhuận thực tế:");
            Cell cell5 = row5.createCell(1);
            cell5.setCellValue(report.getNetRevenue() != null ? report.getNetRevenue().doubleValue() : 0);
            cell5.setCellStyle(currencyStyle);

            // --- 3. BẢNG DANH SÁCH SẢN PHẨM ---
            sheet.createRow(7).createCell(0).setCellValue("DANH SÁCH SẢN PHẨM BÁN CHẠY");
            
            Row headerRow = sheet.createRow(8);
            String[] headers = {"Tên sản phẩm", "Số lượng bán", "Doanh thu"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 9;
            if (report.getTopProducts() != null) {
                for (RevenueReportDTO.ProductSalesDTO prod : report.getTopProducts()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(prod.getProductName());
                    row.createCell(1).setCellValue(prod.getSoldQuantity() != null ? prod.getSoldQuantity() : 0);
                    
                    Cell revCell = row.createCell(2);
                    revCell.setCellValue(prod.getProductRevenue() != null ? prod.getProductRevenue().doubleValue() : 0);
                    revCell.setCellStyle(currencyStyle);
                }
            }

            // Tự động căn chỉnh độ rộng cột
            for (int i = 0; i < 3; i++) sheet.autoSizeColumn(i);

            // --- QUAN TRỌNG: Ghi dữ liệu vào stream ---
            workbook.write(out);
        }
    }
}