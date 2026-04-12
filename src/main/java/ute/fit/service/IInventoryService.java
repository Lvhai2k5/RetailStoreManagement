package ute.fit.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map; // Bắt buộc phải là java.util

public interface IInventoryService {
    // Hàm này phải viết y hệt như thế này
    Map<String, Object> getInventoryStats();
    
    Page<Map<String, Object>> getInventoryList(String keyword, String statusFilter, Pageable pageable);
    
    Map<String, Object> getBatchTraceDetails(Integer batchId);
    
    List<Map<String, Object>> getProductOptions();
    List<Map<String, Object>> getSupplierOptions();
 // Đổi tham số importPrice thành totalImportValue
    void importNewBatch(String productId, Integer supplierId, Integer quantity, Double totalImportValue, Double sellingPrice, String expiryDate);
    Map<String, Object> quickAddProduct(String productId, String name, Integer productTypeId);
    List<Map<String, Object>> getProductTypeOptions();
    Map<String, Object> getBatchForEdit(Integer batchId);
    void updateBatch(Integer batchId, Integer supplierId, Integer quantity, Double importPrice, Double sellingPrice, String expiryDate);
}