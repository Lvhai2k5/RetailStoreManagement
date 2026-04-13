package ute.fit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import ute.fit.service.IInventoryService;

import java.util.Map;

@Controller
public class InventoryController {

    @Autowired private IInventoryService inventoryService;

    @GetMapping("/inventory")
    public String showInventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable, 
            Model model) {
        
        model.addAttribute("stats", inventoryService.getInventoryStats());
        model.addAttribute("batchPage", inventoryService.getInventoryList(keyword, status, pageable));
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentStatus", status);
        
        // Đẩy danh sách Sản phẩm, Nhà cung cấp và Loại sản phẩm sang HTML
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

 // Cập nhật lại @RequestParam trong hàm importNewBatch
    @PostMapping("/inventory/import")
    public String importNewBatch(
            @RequestParam String productId,
            @RequestParam Integer supplierId,
            @RequestParam Integer quantity,
            @RequestParam Double totalImportValue, // Thay đổi ở đây
            @RequestParam Double sellingPrice,
            @RequestParam(required = false) String expiryDate) {
        
        inventoryService.importNewBatch(productId, supplierId, quantity, totalImportValue, sellingPrice, expiryDate);
        return "redirect:/inventory"; 
    }
    
    
    // API xử lý AJAX Thêm Sản Phẩm Nhanh từ Drawer
    @PostMapping("/api/products/quick-add")
    @ResponseBody
    public ResponseEntity<?> quickAddProductAjax(
            @RequestParam String productId,
            @RequestParam String name,
            @RequestParam Integer productTypeId) {
        
        try {
            // Gọi Service để lưu sản phẩm
            Map<String, Object> newProduct = inventoryService.quickAddProduct(productId, name, productTypeId);
            
            // Trả về JSON chứa thông tin sản phẩm vừa tạo thành công (HTTP 200)
            return ResponseEntity.ok(newProduct);
            
        } catch (IllegalArgumentException e) {
            // Nếu có lỗi (Ví dụ: trùng mã ProductID), trả về HTTP 400 Bad Request kèm câu thông báo lỗi
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    
    
 // API 1: Lấy thông tin lô hàng cũ quăng lên Form
    @GetMapping("/api/inventory/batch/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBatchForEditAjax(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(inventoryService.getBatchForEdit(id));
    }

    // API 2: Nhận dữ liệu sửa từ Form
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
            // Bắt lỗi ráng buộc (Ví dụ: Số lượng nhỏ hơn số đã bán) trả về giao diện
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}