package ute.fit.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map; 

public interface IInventoryService {
    
    Map<String, Object> getInventoryStats();
    
    Map<String, Object> getBatchTraceDetails(Integer batchId);
    
    List<Map<String, Object>> getProductOptions();
    
    List<Map<String, Object>> getSupplierOptions();
    
    void importNewBatch(String productId, Integer supplierId, Integer quantity, Double totalImportValue, Double sellingPrice, String expiryDate);
    
    Map<String, Object> quickAddProduct(String productId, String name, Integer productTypeId);
    
    List<Map<String, Object>> getProductTypeOptions();
    
    Map<String, Object> getBatchForEdit(Integer batchId);
    
    void updateBatch(Integer batchId, Integer supplierId, Integer quantity, Double importPrice, Double sellingPrice, String expiryDate);
    
    // Đã giữ lại duy nhất 1 phiên bản hàm có 4 tham số (chứa productTypeId)
    Page<Map<String, Object>> getInventoryList(String keyword, String statusFilter, Integer productTypeId, Pageable pageable);
}