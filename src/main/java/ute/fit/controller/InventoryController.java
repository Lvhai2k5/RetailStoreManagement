package ute.fit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ute.fit.service.IInventoryService;

import java.util.Map;

@Controller
public class InventoryController {

    @Autowired 
    private IInventoryService inventoryService;

    @GetMapping("/inventory")
    public String showInventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer productTypeId, // KHẮC PHỤC LỖI TẠI ĐÂY: Khai báo biến productTypeId
            @PageableDefault(size = 10) Pageable pageable, 
            Model model) {
        
        // Gửi các thống kê tổng quan
        model.addAttribute("stats", inventoryService.getInventoryStats());
        
        // Truyền thêm productTypeId vào service để lọc dữ liệu
        model.addAttribute("batchPage", inventoryService.getInventoryList(keyword, status, productTypeId, pageable));
        
        // Đẩy các giá trị tìm kiếm hiện tại về UI để giữ trạng thái cho các ô Select/Input
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentProductType", productTypeId);
        
        // Đẩy danh sách tùy chọn cho các Modal và Thanh lọc
        model.addAttribute("products", inventoryService.getProductOptions());
        model.addAttribute("suppliers", inventoryService.getSupplierOptions());
        model.addAttribute("productTypes", inventoryService.getProductTypeOptions());
        
        return "Inventory/inventory-management";
    }

    @GetMapping("/inventory/trace/{id}")
    public String traceBatch(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("traceData", inventoryService.getBatchTraceDetails(id));
        return "Inventory/batch-trace";
    }

    @PostMapping("/inventory/import")
    public String importNewBatch(
            @RequestParam String productId,
            @RequestParam Integer supplierId,
            @RequestParam Integer quantity,
            @RequestParam Double totalImportValue, 
            @RequestParam Double sellingPrice,
            @RequestParam(required = false) String expiryDate) {
        
        // Thực hiện nhập kho dựa trên tổng giá trị lô hàng
        inventoryService.importNewBatch(productId, supplierId, quantity, totalImportValue, sellingPrice, expiryDate);
        return "redirect:/inventory"; 
    }
    
    @PostMapping("/api/products/quick-add")
    @ResponseBody
    public ResponseEntity<?> quickAddProductAjax(
            @RequestParam String productId,
            @RequestParam String name,
            @RequestParam Integer productTypeId) {
        
        try {
            // Lưu sản phẩm mới kèm theo loại hàng để áp dụng MarkupPercent sau này
            Map<String, Object> newProduct = inventoryService.quickAddProduct(productId, name, productTypeId);
            return ResponseEntity.ok(newProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    @GetMapping("/api/inventory/batch/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBatchForEditAjax(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(inventoryService.getBatchForEdit(id));
    }

    @PostMapping("/api/inventory/update")
    @ResponseBody
    public ResponseEntity<?> updateBatchAjax(
            @RequestParam Integer batchId,
            @RequestParam Integer supplierId,
            @RequestParam Integer quantity,
            @RequestParam Double importPrice,
            @RequestParam Double sellingPrice,
            @RequestParam(required = false) String expiryDate) {
        
        try {
            inventoryService.updateBatch(batchId, supplierId, quantity, importPrice, sellingPrice, expiryDate);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}