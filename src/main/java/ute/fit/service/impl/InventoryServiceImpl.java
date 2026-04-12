package ute.fit.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ute.fit.entity.ImportBatchesEntity;
import ute.fit.entity.InventoryTransactionsEntity;
import ute.fit.repository.ImportBatchRepository;
import ute.fit.repository.InventoryTransactionRepository;
import ute.fit.service.IInventoryService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryServiceImpl implements IInventoryService {

    @Autowired private ImportBatchRepository batchRepo;
    @Autowired private InventoryTransactionRepository transRepo;

    // --- Hàm 1: Thống kê tổng quan ---
    @Override
    public Map<String, Object> getInventoryStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Đẩy đủ 4 key cho 4 Card thống kê trên UI
        stats.put("totalBatches", batchRepo.count());
        stats.put("expiredCount", batchRepo.countExpired(now));
        stats.put("expiringSoonCount", batchRepo.countExpiringSoon(now, now.plusDays(7)));
        
        Double totalValue = batchRepo.sumTotalInventoryValue();
        stats.put("totalValue", totalValue != null ? totalValue : 0.0);
        
        return stats;
    }

    // --- Hàm 2: Danh sách có Tìm kiếm, Phân trang và Lọc ---
    @Override
    public Page<Map<String, Object>> getInventoryList(String keyword, String statusFilter, Pageable pageable) {
        // Lấy toàn bộ dữ liệu thô
        List<Map<String, Object>> allBatches = batchRepo.findAllBatchesForUI(Pageable.unpaged()).getContent();
        List<Map<String, Object>> filteredList = new ArrayList<>();

        for (Map<String, Object> batch : allBatches) {
            Map<String, Object> row = new HashMap<>(batch);
            Integer batchId = (Integer) batch.get("batchID");
            String productName = (String) batch.get("productName");

            // 1. Tìm kiếm theo Keyword (Mã lô hoặc Tên SP)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchStr = keyword.toLowerCase();
                if (!String.valueOf(batchId).contains(searchStr) && 
                    (productName == null || !productName.toLowerCase().contains(searchStr))) {
                    continue; // Bỏ qua nếu không khớp keyword
                }
            }

            // 2. Tính tồn kho động (3.3.2)
            Integer stockFromTrans = transRepo.sumQuantityChangeByBatchId(batchId);
            int currentStock = (stockFromTrans != null) ? stockFromTrans : 0;
            row.put("availableQty", currentStock);

            // 3. Phân loại trạng thái (3.3.3)
            LocalDateTime expiry = (LocalDateTime) batch.get("expiryDate");
            String status = "AVAILABLE"; 
            if (currentStock <= 0) status = "OUT_OF_STOCK";
            else if (expiry != null) {
                if (expiry.isBefore(LocalDateTime.now())) status = "EXPIRED";
                else if (expiry.isBefore(LocalDateTime.now().plusDays(7))) status = "NEAR_EXPIRY";
            }
            row.put("status", status);

            // 4. Lọc theo Trạng thái
            if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equals(status)) {
                continue; // Bỏ qua nếu không khớp trạng thái lọc
            }

            filteredList.add(row);
        }

        // Phân trang thủ công trên list đã lọc để đảm bảo số lượng trang chính xác
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());
        List<Map<String, Object>> pageContent = (start <= end) ? filteredList.subList(start, end) : new ArrayList<>();
        
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredList.size());
    }

    // --- Hàm 3: Truy vết lịch sử lô hàng (3.3.1) ---
    // Đã thêm @Transactional để cho phép truy cập các Entity Lazy Load (Supplier, Product)
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBatchTraceDetails(Integer batchId) {
        Map<String, Object> result = new HashMap<>();
        
        // Lấy thông tin cơ bản của lô
        ImportBatchesEntity batch = batchRepo.findById(batchId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy lô hàng"));
            
        result.put("batchID", batch.getBatchID());
        result.put("productName", batch.getProduct().getName());
        result.put("supplierName", batch.getSupplier().getName()); // Sẽ không bị lỗi Lazy Load nhờ @Transactional
        result.put("importDate", batch.getImportDate());
        result.put("importQty", batch.getQuantity());
        
        // Lấy lịch sử giao dịch
        List<InventoryTransactionsEntity> trans = transRepo.findByBatchId(batchId);
        List<Map<String, Object>> history = new ArrayList<>();
        
        for (InventoryTransactionsEntity t : trans) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", t.getTransactionDate());
            map.put("type", t.getTransactionType().name()); // SELL, IMPORT, RETURN, DEFECTIVE
            map.put("change", t.getQuantityChange());
            map.put("ref", t.getReferenceID());
            history.add(map);
        }
        
        result.put("history", history);
        return result;
    }
}