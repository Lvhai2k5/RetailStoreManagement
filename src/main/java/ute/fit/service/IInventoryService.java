package ute.fit.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map; // Bắt buộc phải là java.util

public interface IInventoryService {
    // Hàm này phải viết y hệt như thế này
    Map<String, Object> getInventoryStats();
    
    Page<Map<String, Object>> getInventoryList(String keyword, String statusFilter, Pageable pageable);
    
    Map<String, Object> getBatchTraceDetails(Integer batchId);
}